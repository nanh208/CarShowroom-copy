package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.ares.shop.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.*;
import java.util.*;
import java.util.stream.*;

/** v11 — quản trị: thống kê mã giảm, giỏ bỏ quên, phân khúc khách, bố cục trang chủ, nhập hàng, hoàn tiền một phần, ghi chú nội bộ, báo cáo… */
@RestController @RequestMapping("/api/admin")
public class AdminPlusController {
    private final OrderRepo orders; private final ProductRepo products; private final UserRepo users; private final PromotionRepo promos;
    private final AbandonedCartRepo abandoned; private final SettingRepo settings; private final PurchaseOrderRepo pos; private final OrderNoteRepo notes;
    private final EmailLogRepo emails; private final MailService mail; private final OrderService orderService; private final NotifyService notify;
    private final BundleRepo bundles; private final GiftCardRepo giftCards; private final CategoryRepo categories; private final StockMovementSink sink;
    private final StaffDebtRepository staffDebts;
    @org.springframework.beans.factory.annotation.Autowired private com.ares.shop.repo.StaffCommissionTxRepo commissionTxRepo;
    private final PasswordEncoder enc;
    private final ObjectMapper json = new ObjectMapper();

    public AdminPlusController(OrderRepo orders, ProductRepo products, UserRepo users, PromotionRepo promos, AbandonedCartRepo abandoned, SettingRepo settings,
                               PurchaseOrderRepo pos, OrderNoteRepo notes, EmailLogRepo emails, MailService mail, OrderService orderService, NotifyService notify,
                               BundleRepo bundles, GiftCardRepo giftCards, CategoryRepo categories, StaffDebtRepository staffDebts, PasswordEncoder enc) {
        this.orders = orders; this.products = products; this.users = users; this.promos = promos; this.abandoned = abandoned; this.settings = settings;
        this.pos = pos; this.notes = notes; this.emails = emails; this.mail = mail; this.orderService = orderService; this.notify = notify;
        this.bundles = bundles; this.giftCards = giftCards; this.categories = categories; this.staffDebts = staffDebts; this.enc = enc; this.sink = orderService::log;
    }
    interface StockMovementSink { void log(Product p, ProductVariant v, int delta, String reason, String by); }

    private List<CustomerOrder> valid() { return orders.findAll().stream().filter(o -> o.status != Enums.OrderStatus.CANCELLED).toList(); }

    // ===== Thống kê mã giảm giá =====
    @GetMapping("/promotions/stats")
    public List<Map<String, Object>> promoStats() {
        Map<String, List<CustomerOrder>> by = valid().stream().filter(o -> o.promoCode != null).collect(Collectors.groupingBy(o -> o.promoCode.toUpperCase()));
        return promos.findAll().stream().map(p -> {
            var l = by.getOrDefault(p.code.toUpperCase(), List.of());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.id); m.put("code", p.code); m.put("title", p.title); m.put("kind", p.kind); m.put("active", p.isValidNow());
            m.put("usedCount", p.usedCount); m.put("usageLimit", p.usageLimit); m.put("orders", l.size());
            m.put("revenue", l.stream().mapToLong(o -> o.total).sum()); m.put("discountGiven", l.stream().mapToLong(o -> o.discount).sum());
            m.put("customers", l.stream().map(o -> o.user == null ? o.email : o.user.id).filter(Objects::nonNull).distinct().count());
            m.put("aov", l.isEmpty() ? 0 : l.stream().mapToLong(o -> o.total).sum() / l.size());
            return m;
        }).sorted(Comparator.comparing((Map<String, Object> m) -> (Integer) m.get("orders")).reversed()).toList();
    }

    // ===== Giỏ hàng bị bỏ quên =====
    @GetMapping("/abandoned-carts")
    public List<AbandonedCart> abandonedCarts(@RequestParam(defaultValue = "30") int minutes) {
        var cut = LocalDateTime.now().minusMinutes(minutes);
        return abandoned.findAllByOrderByLastSeenDesc().stream().filter(c -> c.lastSeen.isBefore(cut)).toList();
    }
    @PostMapping("/abandoned-carts/{id}/remind")
    public Map<String, Object> remind(@PathVariable Long id) {
        AbandonedCart c = abandoned.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy giỏ"));
        var e = mail.send(c.email, "Bạn còn quên vài món trong giỏ ARES Toys 🏁", "CART_REMINDER",
                "Chào " + Optional.ofNullable(c.name).orElse("bạn") + ",\nGiỏ hàng trị giá " + MailService.vnd(c.subtotal) + " vẫn đang chờ bạn. Hoàn tất đơn trong 24h với mã COMEBACK5 (giảm 5%).\nTiếp tục: /cart");
        if (c.userId != null) users.findById(c.userId).ifPresent(u -> notify.send(u, "SYSTEM", "Giỏ hàng đang chờ bạn", "Hoàn tất đơn ngay để giữ hàng.", "/cart"));
        return Map.of("sentTo", e == null ? "" : e.toAddr);
    }

    // ===== Phân khúc khách hàng (RFM rút gọn) =====
    @GetMapping("/segments")
    public Map<String, Object> segments() {
        var byUser = valid().stream().filter(o -> o.user != null).collect(Collectors.groupingBy(o -> o.user.id));
        var now = LocalDateTime.now();
        Map<String, List<Map<String, Object>>> seg = new LinkedHashMap<>();
        for (String k : List.of("VIP", "LOYAL", "NEW", "AT_RISK", "NO_ORDER")) seg.put(k, new ArrayList<>());
        for (User u : users.findAll()) {
            if (u.role != Enums.Role.USER) continue;
            var l = byUser.getOrDefault(u.id, List.of());
            long spent = l.stream().filter(o -> o.status == Enums.OrderStatus.COMPLETED).mapToLong(o -> o.total).sum();
            var last = l.stream().map(o -> o.createdAt).max(Comparator.naturalOrder()).orElse(null);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.id); m.put("name", u.fullName); m.put("email", u.email); m.put("orders", l.size()); m.put("spent", spent); m.put("lastOrder", last); m.put("tier", u.tier);
            String k = l.isEmpty() ? "NO_ORDER" : spent >= 10_000_000 ? "VIP" : last.isBefore(now.minusDays(60)) ? "AT_RISK" : l.size() >= 3 ? "LOYAL" : "NEW";
            m.put("segment", k); seg.get(k).add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        seg.forEach((k, v) -> out.put(k, Map.of("count", v.size(), "spent", v.stream().mapToLong(x -> (long) x.get("spent")).sum(), "members", v)));
        return out;
    }

    // ===== Bố cục trang chủ =====
    @PutMapping("/home-layout")
    public Object saveLayout(@RequestBody List<Map<String, Object>> l) throws Exception {
        var keys = l.stream().map(x -> String.valueOf(x.get("key"))).toList();
        if (!new HashSet<>(ShopPlusController.HOME_SECTIONS).containsAll(keys)) throw ApiException.bad("Mục không hợp lệ");
        List<Map<String, Object>> clean = l.stream().map(x -> Map.<String, Object>of("key", String.valueOf(x.get("key")), "visible", !Boolean.FALSE.equals(x.get("visible")))).collect(Collectors.toList());
        for (String k : ShopPlusController.HOME_SECTIONS) if (!keys.contains(k)) clean.add(Map.of("key", k, "visible", true));
        AppSetting s = new AppSetting(); s.k = "homeLayout"; s.v = json.writeValueAsString(clean); settings.save(s);
        return clean;
    }

    // ===== Gợi ý nhập hàng + đơn đặt nhà cung cấp =====
    @GetMapping("/restock-suggestions")
    public List<Map<String, Object>> restock(@RequestParam(defaultValue = "30") int days, @RequestParam(defaultValue = "21") int coverDays) {
        var cut = LocalDateTime.now().minusDays(days);
        Map<Long, Integer> sold = new HashMap<>();
        for (CustomerOrder o : valid()) if (o.createdAt.isAfter(cut)) for (OrderItem i : o.items) if (i.productId != null) sold.merge(i.productId, i.quantity, Integer::sum);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Product p : products.findAll()) {
            if (!p.active) continue;
            int s = sold.getOrDefault(p.id, 0); double perDay = s / (double) days;
            int target = (int) Math.ceil(Math.max(perDay * coverDays, 6)); int need = target - p.stock;
            if (p.stock > AlertService.LOW_STOCK && need <= 0) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productId", p.id); m.put("sku", p.sku); m.put("name", p.name); m.put("image", p.images.isEmpty() ? "" : p.images.get(0)); m.put("stock", p.stock);
            m.put("sold", s); m.put("perDay", Math.round(perDay * 100) / 100.0); m.put("daysLeft", perDay > 0 ? (int) (p.stock / perDay) : null);
            m.put("suggestQty", Math.max(need, p.stock <= AlertService.LOW_STOCK ? 10 - p.stock : 0)); m.put("unitCost", Math.round(p.price * 0.6 / 1000) * 1000);
            out.add(m);
        }
        out.sort(Comparator.comparing(m -> (Integer) m.get("stock")));
        return out;
    }
    @GetMapping("/purchase-orders") public List<PurchaseOrder> poList() { return pos.findAllByOrderByCreatedAtDesc(); }
    public record PoLine(Long productId, int qty, long unitCost) {}
    public record PoReq(String supplier, String note, List<PoLine> items) {}
    @PostMapping("/purchase-orders")
    public PurchaseOrder createPo(@RequestBody PoReq r) throws Exception {
        if (r.items() == null || r.items().isEmpty()) throw ApiException.bad("Đơn nhập trống");
        List<Map<String, Object>> lines = new ArrayList<>(); long total = 0;
        for (PoLine l : r.items()) {
            if (l.qty() <= 0) continue;
            Product p = products.findById(l.productId()).orElseThrow(() -> ApiException.bad("Sản phẩm không tồn tại"));
            lines.add(Map.of("productId", p.id, "sku", p.sku, "name", p.name, "qty", l.qty(), "unitCost", l.unitCost())); total += l.qty() * l.unitCost();
        }
        PurchaseOrder po = new PurchaseOrder(); if (r.supplier() != null && !r.supplier().isBlank()) po.supplier = r.supplier().trim(); po.note = r.note();
        po.itemsJson = json.writeValueAsString(lines); po.totalCost = total; return pos.save(po);
    }
    @PostMapping("/purchase-orders/{id}/status") @Transactional
    public PurchaseOrder poStatus(@PathVariable Long id, @RequestBody Map<String, String> b) throws Exception {
        PurchaseOrder po = pos.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn nhập"));
        String s = Optional.ofNullable(b.get("status")).orElse("");
        Map<String, Set<String>> flow = Map.of("DRAFT", Set.of("ORDERED", "CANCELLED"), "ORDERED", Set.of("RECEIVED", "CANCELLED"));
        if (!flow.getOrDefault(po.status, Set.of()).contains(s)) throw ApiException.bad("Không thể chuyển " + po.status + " → " + s);
        if (s.equals("RECEIVED")) {
            List<Map<String, Object>> lines = json.readValue(po.itemsJson, json.getTypeFactory().constructCollectionType(List.class, Map.class));
            for (var l : lines) products.findById(((Number) l.get("productId")).longValue()).ifPresent(p -> {
                int q = ((Number) l.get("qty")).intValue(); p.stock += q; products.save(p); sink.log(p, null, q, "Nhập hàng PO#" + po.id, Current.user().email); });
            po.receivedAt = LocalDateTime.now();
        }
        po.status = s; return pos.save(po);
    }

    // ===== Hoàn tiền một phần =====
    public record RefundReq(long amount, String reason, Boolean toCredit) {}
    @PostMapping("/orders/{id}/refund") @Transactional
    public CustomerOrder refund(@PathVariable Long id, @RequestBody RefundReq r) {
        CustomerOrder o = orders.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn"));
        long done = Optional.ofNullable(o.refundedAmount).orElse(0L), max = o.paidAmount - done;
        if (o.paidAmount <= 0) throw ApiException.bad("Đơn chưa thanh toán — không thể hoàn tiền");
        if (r.amount() <= 0 || r.amount() > max) throw ApiException.bad("Số tiền hoàn phải từ 1đ đến " + MailService.vnd(max));
        if (r.reason() == null || r.reason().isBlank()) throw ApiException.bad("Vui lòng nhập lý do hoàn tiền");
        o.refundedAmount = done + r.amount();
        if (o.refundedAmount >= o.paidAmount) o.paymentStatus = Enums.PaymentStatus.REFUNDED;
        o.timeline.put("REFUND_" + (o.timeline.keySet().stream().filter(k -> k.startsWith("REFUND_")).count() + 1), LocalDateTime.now());
        boolean credit = Boolean.TRUE.equals(r.toCredit()) && o.user != null;
        if (credit) users.findById(o.user.id).ifPresent(u -> { u.storeCredit = Optional.ofNullable(u.storeCredit).orElse(0L) + r.amount(); users.save(u); });
        addNote(o.id, "Hoàn " + MailService.vnd(r.amount()) + (credit ? " vào ví tín dụng" : " về phương thức gốc") + " — " + r.reason().trim());
        notify.send(o.user, "ORDER", "Hoàn tiền đơn " + o.code, "Đã hoàn " + MailService.vnd(r.amount()) + (credit ? " vào ví tín dụng ARES." : ". Tiền về tài khoản trong 3–5 ngày làm việc."), "/orders/" + o.code);
        mail.send(o.email, "Hoàn tiền đơn " + o.code, "REFUND", "Chúng tôi đã hoàn " + MailService.vnd(r.amount()) + " cho đơn " + o.code + ".\nLý do: " + r.reason().trim());
        return orders.save(o);
    }

    // ===== Ghi chú nội bộ =====
    @GetMapping("/orders/{id}/notes") public List<OrderNote> notes(@PathVariable Long id) { return notes.findByOrderIdOrderByCreatedAtAsc(id); }
    @PostMapping("/orders/{id}/notes")
    public OrderNote note(@PathVariable Long id, @RequestBody Map<String, String> b) {
        orders.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn"));
        String t = Optional.ofNullable(b.get("text")).orElse("").trim(); if (t.isEmpty()) throw ApiException.bad("Ghi chú trống");
        return addNote(id, t.substring(0, Math.min(2000, t.length())));
    }
    private OrderNote addNote(Long orderId, String text) { OrderNote n = new OrderNote(); n.orderId = orderId; n.author = Current.user().fullName; n.text = text; return notes.save(n); }

    // ===== Báo cáo doanh số theo danh mục / thương hiệu =====
    @GetMapping("/analytics/sales-by")
    public List<Map<String, Object>> salesBy(@RequestParam(defaultValue = "category") String dim, @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        LocalDate f = from == null || from.isBlank() ? LocalDate.now().minusDays(29) : LocalDate.parse(from), t = to == null || to.isBlank() ? LocalDate.now() : LocalDate.parse(to);
        Map<String, String> catName = new HashMap<>(); categories.findAll().forEach(c -> catName.put(c.code, c.name));
        Map<Long, Product> pm = products.findAll().stream().collect(Collectors.toMap(p -> p.id, p -> p));
        Map<String, long[]> agg = new LinkedHashMap<>();
        for (CustomerOrder o : valid()) {
            var d = o.createdAt.toLocalDate(); if (d.isBefore(f) || d.isAfter(t)) continue;
            for (OrderItem i : o.items) {
                Product p = i.productId == null ? null : pm.get(i.productId);
                String key = p == null ? "Khác" : dim.equals("brand") ? (p.brand == null ? "Khác" : p.brand.name) : catName.getOrDefault(p.type, p.type == null ? "Khác" : p.type);
                long[] a = agg.computeIfAbsent(key, k -> new long[3]); a[0] += i.unitPrice * i.quantity; a[1] += i.quantity; a[2]++;
            }
        }
        long sum = agg.values().stream().mapToLong(a -> a[0]).sum();
        return agg.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0])).map(e -> {
            Map<String, Object> m = new LinkedHashMap<>(); m.put("name", e.getKey()); m.put("revenue", e.getValue()[0]); m.put("qty", e.getValue()[1]); m.put("lines", e.getValue()[2]);
            m.put("share", sum == 0 ? 0 : Math.round(e.getValue()[0] * 1000.0 / sum) / 10.0); return m; }).toList();
    }

    // ===== Hộp thư đi (email mô phỏng) =====
    @GetMapping("/emails") public List<EmailLog> outbox() { return emails.findTop200ByOrderByCreatedAtDesc(); }

    // ===== Combo / Bundle =====
    @GetMapping("/bundles") public List<Bundle> bundleList() { return bundles.findAll(); }
    @PostMapping("/bundles") public Bundle createBundle(@RequestBody Bundle b) { b.id = null; return bundles.save(checkBundle(b)); }
    @PutMapping("/bundles/{id}") public Bundle updateBundle(@PathVariable Long id, @RequestBody Bundle b) { b.id = id; return bundles.save(checkBundle(b)); }
    @DeleteMapping("/bundles/{id}") public void delBundle(@PathVariable Long id) { bundles.deleteById(id); }
    private Bundle checkBundle(Bundle b) {
        if (b.title == null || b.title.isBlank()) throw ApiException.bad("Nhập tên combo");
        if (b.productIds == null || b.productIds.isEmpty()) throw ApiException.bad("Chọn ít nhất 1 sản phẩm");
        if ("FIXED".equals(b.kind) && (b.bundlePrice <= 0 || b.productIds.size() < 2)) throw ApiException.bad("Combo giá gói cần ≥ 2 sản phẩm và giá > 0");
        if (!"FIXED".equals(b.kind) && (b.buyQty < 1 || b.getQty < 1)) throw ApiException.bad("Mua X tặng Y: X, Y ≥ 1");
        return b;
    }

    // ===== Thẻ quà (phát hành thủ công) =====
    @GetMapping("/giftcards") public List<GiftCard> giftList() { return giftCards.findAll(); }
    @PostMapping("/giftcards")
    public GiftCard issueGift(@RequestBody Map<String, Object> b) {
        long amt = ((Number) b.getOrDefault("amount", 0)).longValue(); if (amt < 10_000 || amt > 50_000_000) throw ApiException.bad("Mệnh giá 10.000đ – 50.000.000đ");
        GiftCard g = new GiftCard(); g.code = ShopPlusController.genCode("GC"); g.initial = amt; g.balance = amt; g.message = (String) b.get("message"); g.expiresAt = LocalDateTime.now().plusYears(1);
        giftCards.save(g);
        Object to = b.get("email"); if (to != null && !to.toString().isBlank()) mail.send(to.toString(), "Thẻ quà ARES Toys " + MailService.vnd(amt), "GIFT_CARD", "Mã thẻ: " + g.code + "\nGiá trị: " + MailService.vnd(amt));
        return g;
    }

    // ===== Công nợ nhân viên =====
    @GetMapping("/staff-debts")
    public Map<String, Object> listStaffDebts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long staffId,
            @RequestParam(required = false) String q) {
        List<StaffDebt> all = staffDebts.findAllByOrderByCreatedAtDesc();
        List<StaffDebt> filtered = all.stream().filter(d -> {
            if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL") && !d.status.equalsIgnoreCase(status)) return false;
            if (staffId != null && (d.staffId == null || !d.staffId.equals(staffId))) return false;
            if (q != null && !q.isBlank()) {
                String lq = q.toLowerCase();
                boolean mName = d.staffName != null && d.staffName.toLowerCase().contains(lq);
                boolean mEmail = d.staffEmail != null && d.staffEmail.toLowerCase().contains(lq);
                boolean mReason = d.reason != null && d.reason.toLowerCase().contains(lq);
                if (!mName && !mEmail && !mReason) return false;
            }
            return true;
        }).toList();

        long totalDebt = all.stream().mapToLong(d -> d.amount).sum();
        long totalPaid = all.stream().mapToLong(d -> d.paidAmount).sum();
        long totalRemaining = all.stream().mapToLong(d -> d.remainingAmount).sum();
        long activeStaffCount = all.stream().filter(d -> d.remainingAmount > 0).map(d -> d.staffId).filter(Objects::nonNull).distinct().count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalDebt", totalDebt);
        stats.put("totalPaid", totalPaid);
        stats.put("totalRemaining", totalRemaining);
        stats.put("activeStaffCount", activeStaffCount);
        stats.put("totalRecords", all.size());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("items", filtered);
        res.put("stats", stats);
        return res;
    }

    @GetMapping("/staff-debts/staff-list")
    public List<Map<String, Object>> staffList() {
        return users.findAll().stream()
                .filter(u -> u.role == Enums.Role.STAFF || u.role == Enums.Role.ADMIN)
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", u.id);
                    m.put("fullName", u.fullName);
                    m.put("email", u.email);
                    m.put("phone", u.phone);
                    m.put("role", u.role);
                    m.put("avatarUrl", u.avatarUrl);
                    return m;
                }).toList();
    }

    @PostMapping("/staff-debts")
    public StaffDebt createStaffDebt(@RequestBody Map<String, Object> body) {
        Long staffId = ((Number) body.get("staffId")).longValue();
        User staff = users.findById(staffId).orElseThrow(() -> ApiException.bad("Không tìm thấy nhân viên"));
        long amount = ((Number) body.get("amount")).longValue();
        if (amount <= 0) throw ApiException.bad("Số tiền nợ phải lớn hơn 0");

        StaffDebt d = new StaffDebt();
        d.staffId = staff.id;
        d.staffName = staff.fullName;
        d.staffEmail = staff.email;
        d.debtType = (String) body.getOrDefault("debtType", "TAM_UNG");
        d.amount = amount;
        d.paidAmount = 0;
        d.remainingAmount = amount;
        d.status = "UNPAID";
        d.reason = (String) body.get("reason");
        String dueStr = (String) body.get("dueDate");
        if (dueStr != null && !dueStr.isBlank()) {
            try { d.dueDate = LocalDate.parse(dueStr); } catch (Exception ignored) {}
        }
        User me = Current.user();
        d.createdBy = me != null ? me.fullName : "Admin";
        return staffDebts.save(d);
    }

    @PostMapping("/staff-debts/{id}/repay")
    public StaffDebt repayStaffDebt(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        StaffDebt d = staffDebts.findById(id).orElseThrow(() -> ApiException.bad("Không tìm thấy bản ghi công nợ"));
        long payAmount = ((Number) body.get("payAmount")).longValue();
        if (payAmount <= 0) throw ApiException.bad("Số tiền trả phải lớn hơn 0");
        if (payAmount > d.remainingAmount) throw ApiException.bad("Số tiền trả vượt quá số dư nợ còn lại (" + d.remainingAmount + "đ)");

        d.paidAmount += payAmount;
        d.remainingAmount = d.amount - d.paidAmount;
        d.status = d.remainingAmount <= 0 ? "PAID" : "PARTIAL";
        d.updatedAt = LocalDateTime.now();

        String method = (String) body.getOrDefault("method", "CHUYEN_KHOAN");
        String note = (String) body.getOrDefault("note", "");

        List<Map<String, Object>> hist = new ArrayList<>();
        if (d.paymentHistoryJson != null && !d.paymentHistoryJson.isBlank()) {
            try {
                hist = json.readValue(d.paymentHistoryJson, List.class);
            } catch (Exception ignored) {}
        }
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("paidAt", LocalDateTime.now().toString());
        entry.put("amount", payAmount);
        entry.put("method", method);
        entry.put("note", note);
        User me = Current.user();
        entry.put("recordedBy", me != null ? me.fullName : "Admin");
        hist.add(entry);
        try {
            d.paymentHistoryJson = json.writeValueAsString(hist);
        } catch (Exception ignored) {}

        return staffDebts.save(d);
    }

    @DeleteMapping("/staff-debts/{id}")
    public void deleteStaffDebt(@PathVariable Long id) {
        if (Current.user().role != Enums.Role.ADMIN) throw ApiException.bad("Chỉ quản trị viên được xoá công nợ");
        staffDebts.deleteById(id);
    }

    // ===== Quản lý nhân viên (Staff Management) =====
    @GetMapping("/staff")
    public List<Map<String, Object>> listStaff() {
        var allDebts = staffDebts.findAll();
        var allOrders = orders.findAll();
        return users.findAll().stream()
                .filter(u -> u.role == Enums.Role.STAFF || u.role == Enums.Role.ADMIN)
                .sorted(Comparator.comparing(u -> u.id))
                .map(u -> {
                    long remainingDebt = allDebts.stream()
                            .filter(d -> d.staffId != null && d.staffId.equals(u.id))
                            .mapToLong(d -> d.remainingAmount).sum();
                    long totalDebt = allDebts.stream()
                            .filter(d -> d.staffId != null && d.staffId.equals(u.id))
                            .mapToLong(d -> d.amount).sum();
                    long debtCount = allDebts.stream()
                            .filter(d -> d.staffId != null && d.staffId.equals(u.id) && d.remainingAmount > 0).count();

                    var staffOrders = allOrders.stream()
                            .filter(o -> o.staffId != null && o.staffId.equals(u.id) && o.status != Enums.OrderStatus.CANCELLED)
                            .toList();
                    long posSales = staffOrders.stream().filter(o -> "POS".equalsIgnoreCase(o.channel)).mapToLong(o -> o.total).sum();
                    long onlineSales = staffOrders.stream().filter(o -> !"POS".equalsIgnoreCase(o.channel)).mapToLong(o -> o.total).sum();
                    long totalSales = posSales + onlineSales;

                    var commTxs = commissionTxRepo != null ? commissionTxRepo.findByStaffIdOrderByCreatedAtDesc(u.id) : List.<com.ares.shop.model.StaffCommissionTx>of();
                    long earnedCommission = commTxs.stream().filter(t -> "EARN".equalsIgnoreCase(t.txType)).mapToLong(t -> t.amount).sum();
                    long paidCommissionTxs = commTxs.stream().filter(t -> "PAYOUT".equalsIgnoreCase(t.txType)).mapToLong(t -> t.amount).sum();
                    long paidCommission = (u.totalCommissionPaid != null ? u.totalCommissionPaid : 0L) + paidCommissionTxs;
                    long remainingCommission = Math.max(0, earnedCommission - paidCommission);

                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", u.id);
                    m.put("fullName", u.fullName);
                    m.put("email", u.email);
                    m.put("phone", u.phone);
                    m.put("role", u.role);
                    m.put("locked", u.locked);
                    m.put("avatarUrl", u.avatarUrl);
                    m.put("createdAt", u.createdAt);
                    m.put("staffNote", u.staffNote);
                    m.put("staffCode", u.staffCode != null ? u.staffCode : ("NV-" + u.id));
                    m.put("department", u.department != null ? u.department : "Tư vấn bán hàng");
                    m.put("commissionRate", u.commissionRate != null ? u.commissionRate : 2.0);
                    m.put("monthlySalesTarget", u.monthlySalesTarget != null ? u.monthlySalesTarget : 50_000_000L);
                    m.put("remainingDebt", remainingDebt);
                    m.put("totalDebt", totalDebt);
                    m.put("debtCount", debtCount);
                    m.put("posSales", posSales);
                    m.put("onlineSales", onlineSales);
                    m.put("totalSales", totalSales);
                    m.put("orderCount", staffOrders.size());
                    m.put("earnedCommission", earnedCommission);
                    m.put("paidCommission", paidCommission);
                    m.put("remainingCommission", remainingCommission);
                    return m;
                }).toList();
    }

    @PostMapping("/staff/commission/payout")
    @Transactional
    public Map<String, Object> payoutCommission(@RequestBody Map<String, Object> body) {
        Long staffId = Long.valueOf(body.get("staffId").toString());
        long amount = Long.parseLong(body.get("amount").toString());
        String note = body.get("note") != null ? body.get("note").toString() : "Chi trả hoa hồng";

        User u = users.findById(staffId).orElseThrow(() -> ApiException.notFound("Không tìm thấy nhân viên"));

        com.ares.shop.model.StaffCommissionTx tx = new com.ares.shop.model.StaffCommissionTx();
        tx.staffId = u.id;
        tx.staffName = u.fullName;
        tx.txType = "PAYOUT";
        tx.amount = amount;
        tx.note = note;
        tx.createdBy = Current.user() != null ? Current.user().fullName : "ADMIN";
        tx.createdAt = LocalDateTime.now();
        if (commissionTxRepo != null) commissionTxRepo.save(tx);

        u.totalCommissionPaid = (u.totalCommissionPaid != null ? u.totalCommissionPaid : 0L) + amount;
        users.save(u);

        return Map.of("success", true, "message", "Đã quyết toán " + amount + "đ cho nhân viên " + u.fullName);
    }

    @PostMapping("/staff")
    public User createStaff(@RequestBody Map<String, Object> body) {
        String email = (String) body.get("email");
        if (email == null || email.isBlank()) throw ApiException.bad("Email nhân viên bắt buộc");
        email = email.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw ApiException.bad("Email [" + email + "] đã tồn tại");

        String fullName = (String) body.get("fullName");
        if (fullName == null || fullName.isBlank()) throw ApiException.bad("Họ tên nhân viên bắt buộc");

        String rawPassword = (String) body.get("password");
        if (rawPassword == null || rawPassword.length() < 6) throw ApiException.bad("Mật khẩu phải từ 6 ký tự");

        String roleStr = (String) body.getOrDefault("role", "STAFF");
        Enums.Role role = Enums.Role.STAFF;
        try {
            role = Enums.Role.valueOf(roleStr.toUpperCase());
        } catch (Exception ignored) {}

        User u = new User();
        u.email = email;
        u.fullName = fullName.trim();
        u.phone = (String) body.get("phone");
        u.password = enc.encode(rawPassword);
        u.role = role;
        u.provider = "LOCAL";
        u.locked = false;
        u.createdAt = LocalDateTime.now();
        u.staffNote = (String) body.get("staffNote");
        if (body.get("staffCode") != null) u.staffCode = body.get("staffCode").toString().trim();
        if (body.get("department") != null) u.department = body.get("department").toString().trim();
        if (body.get("commissionRate") != null) u.commissionRate = Double.parseDouble(body.get("commissionRate").toString());
        if (body.get("monthlySalesTarget") != null) u.monthlySalesTarget = Long.parseLong(body.get("monthlySalesTarget").toString());
        if (body.get("employmentStatus") != null) u.employmentStatus = body.get("employmentStatus").toString().trim();
        if (body.get("hireDate") != null && !body.get("hireDate").toString().isBlank()) {
            try { u.hireDate = java.time.LocalDate.parse(body.get("hireDate").toString()); } catch (Exception ignored) {}
        }
        return users.save(u);
    }

    @PutMapping("/staff/{id}")
    public User updateStaff(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy nhân viên"));
        if (body.containsKey("fullName")) {
            String name = (String) body.get("fullName");
            if (name != null && !name.isBlank()) u.fullName = name.trim();
        }
        if (body.containsKey("phone")) {
            u.phone = (String) body.get("phone");
        }
        if (body.containsKey("staffNote")) {
            u.staffNote = (String) body.get("staffNote");
        }
        if (body.containsKey("role")) {
            String roleStr = (String) body.get("role");
            if (roleStr != null && !roleStr.isBlank()) {
                if (u.id.equals(Current.user().id) && !u.role.name().equalsIgnoreCase(roleStr)) {
                    throw ApiException.bad("Không thể tự đổi vai trò của chính mình");
                }
                try {
                    u.role = Enums.Role.valueOf(roleStr.toUpperCase());
                } catch (Exception ignored) {}
            }
        }
        if (body.containsKey("password")) {
            String newPass = (String) body.get("password");
            if (newPass != null && !newPass.isBlank()) {
                if (newPass.length() < 6) throw ApiException.bad("Mật khẩu mới phải từ 6 ký tự");
                u.password = enc.encode(newPass);
            }
        }
        return users.save(u);
    }

    @PatchMapping("/staff/{id}/toggle-lock")
    public User toggleLockStaff(@PathVariable Long id) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy nhân viên"));
        if (u.id.equals(Current.user().id)) {
            throw ApiException.bad("Không thể tự khoá tài khoản của chính mình");
        }
        u.locked = !u.locked;
        return users.save(u);
    }

    @DeleteMapping("/staff/{id}")
    public void deleteStaff(@PathVariable Long id) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy nhân viên"));
        if (u.id.equals(Current.user().id)) {
            throw ApiException.bad("Không thể tự xoá tài khoản của chính mình");
        }
        boolean hasDebt = staffDebts.findAll().stream().anyMatch(d -> d.staffId != null && d.staffId.equals(u.id) && d.remainingAmount > 0);
        if (hasDebt) {
            throw ApiException.bad("Nhân viên này vẫn còn dư nợ công nợ chưa quyết toán");
        }
        users.delete(u);
    }

    // ===== Quyết toán hoa hồng nhân viên =====
    @PostMapping("/staff/{id}/payout-commission")
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> payoutCommission(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy nhân viên"));
        long amount = body.get("amount") != null ? Long.parseLong(body.get("amount").toString()) : 0L;
        if (amount <= 0) throw ApiException.bad("Số tiền quyết toán hoa hồng phải lớn hơn 0");

        String note = body.get("note") != null ? body.get("note").toString() : "Chi trả hoa hồng bán hàng";
        User cur = Current.user();

        com.ares.shop.model.StaffCommissionTx tx = new com.ares.shop.model.StaffCommissionTx();
        tx.staffId = u.id;
        tx.staffName = u.fullName;
        tx.txType = "PAYOUT";
        tx.amount = amount;
        tx.note = note;
        tx.createdBy = cur != null ? cur.fullName : "Kế toán / Quản lý";
        tx.createdAt = java.time.LocalDateTime.now();
        if (commissionTxRepo != null) commissionTxRepo.save(tx);

        u.totalCommissionPaid = (u.totalCommissionPaid != null ? u.totalCommissionPaid : 0L) + amount;
        users.save(u);

        return Map.of("success", true, "staffId", u.id, "staffName", u.fullName, "paidAmount", amount);
    }

    @GetMapping("/staff/{id}/commissions")
    public List<com.ares.shop.model.StaffCommissionTx> getStaffCommissions(@PathVariable Long id) {
        if (commissionTxRepo == null) return List.of();
        return commissionTxRepo.findByStaffIdOrderByCreatedAtDesc(id);
    }

    @GetMapping("/staff/commissions")
    public List<com.ares.shop.model.StaffCommissionTx> getAllCommissions() {
        if (commissionTxRepo == null) return List.of();
        return commissionTxRepo.findAllByOrderByCreatedAtDesc();
    }

    // ===== Thống kê phân chia sản lượng On & Off =====
    @GetMapping("/analytics/channels")
    public Map<String, Object> getChannelAnalytics(@RequestParam(required = false) String from,
                                                   @RequestParam(required = false) String to) {
        var all = orders.findAll();
        java.time.LocalDate fromDate = from != null && !from.isBlank() ? java.time.LocalDate.parse(from) : java.time.LocalDate.now().minusDays(30);
        java.time.LocalDate toDate = to != null && !to.isBlank() ? java.time.LocalDate.parse(to) : java.time.LocalDate.now();

        var periodOrders = all.stream()
                .filter(o -> o.status != Enums.OrderStatus.CANCELLED)
                .filter(o -> o.createdAt != null)
                .filter(o -> !o.createdAt.toLocalDate().isBefore(fromDate) && !o.createdAt.toLocalDate().isAfter(toDate))
                .toList();

        var onlineList = periodOrders.stream().filter(o -> !"POS".equalsIgnoreCase(o.channel)).toList();
        var posList = periodOrders.stream().filter(o -> "POS".equalsIgnoreCase(o.channel)).toList();

        long onlineRev = onlineList.stream().mapToLong(o -> o.paidAmount > 0 ? o.paidAmount : o.total).sum();
        long posRev = posList.stream().mapToLong(o -> o.paidAmount > 0 ? o.paidAmount : o.total).sum();
        long totalRev = onlineRev + posRev;

        long onlineOrders = onlineList.size();
        long posOrders = posList.size();
        long totalOrders = onlineOrders + posOrders;

        long onlineItems = onlineList.stream().flatMap(o -> o.items.stream()).mapToLong(i -> i.quantity).sum();
        long posItems = posList.stream().flatMap(o -> o.items.stream()).mapToLong(i -> i.quantity).sum();
        long totalItems = onlineItems + posItems;

        double onlineRevShare = totalRev > 0 ? Math.round((onlineRev * 1000.0) / totalRev) / 10.0 : 0.0;
        double posRevShare = totalRev > 0 ? Math.round((posRev * 1000.0) / totalRev) / 10.0 : 0.0;

        double onlineAov = onlineOrders > 0 ? Math.round((double) onlineRev / onlineOrders) : 0.0;
        double posAov = posOrders > 0 ? Math.round((double) posRev / posOrders) : 0.0;

        // Thống kê theo ngày
        List<Map<String, Object>> dailyStats = new ArrayList<>();
        java.time.LocalDate cur = fromDate;
        while (!cur.isAfter(toDate)) {
            final java.time.LocalDate d = cur;
            long onDayRev = onlineList.stream().filter(o -> o.createdAt.toLocalDate().equals(d)).mapToLong(o -> o.total).sum();
            long posDayRev = posList.stream().filter(o -> o.createdAt.toLocalDate().equals(d)).mapToLong(o -> o.total).sum();
            long onDayItems = onlineList.stream().filter(o -> o.createdAt.toLocalDate().equals(d)).flatMap(o -> o.items.stream()).mapToLong(i -> i.quantity).sum();
            long posDayItems = posList.stream().filter(o -> o.createdAt.toLocalDate().equals(d)).flatMap(o -> o.items.stream()).mapToLong(i -> i.quantity).sum();

            dailyStats.add(Map.of(
                    "date", d.toString(),
                    "onlineRevenue", onDayRev,
                    "posRevenue", posDayRev,
                    "onlineItems", onDayItems,
                    "posItems", posDayItems,
                    "totalRevenue", onDayRev + posDayRev
            ));
            cur = cur.plusDays(1);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("fromDate", fromDate.toString());
        res.put("toDate", toDate.toString());
        res.put("totalRevenue", totalRev);
        res.put("onlineRevenue", onlineRev);
        res.put("posRevenue", posRev);
        res.put("onlineRevShare", onlineRevShare);
        res.put("posRevShare", posRevShare);
        res.put("totalOrders", totalOrders);
        res.put("onlineOrders", onlineOrders);
        res.put("posOrders", posOrders);
        res.put("totalItems", totalItems);
        res.put("onlineItems", onlineItems);
        res.put("posItems", posItems);
        res.put("onlineAov", onlineAov);
        res.put("posAov", posAov);
        res.put("dailyStats", dailyStats);

        return res;
    }
}
