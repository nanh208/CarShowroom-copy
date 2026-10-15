package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.ares.shop.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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
    private final ObjectMapper json = new ObjectMapper();

    public AdminPlusController(OrderRepo orders, ProductRepo products, UserRepo users, PromotionRepo promos, AbandonedCartRepo abandoned, SettingRepo settings,
                               PurchaseOrderRepo pos, OrderNoteRepo notes, EmailLogRepo emails, MailService mail, OrderService orderService, NotifyService notify,
                               BundleRepo bundles, GiftCardRepo giftCards, CategoryRepo categories) {
        this.orders = orders; this.products = products; this.users = users; this.promos = promos; this.abandoned = abandoned; this.settings = settings;
        this.pos = pos; this.notes = notes; this.emails = emails; this.mail = mail; this.orderService = orderService; this.notify = notify;
        this.bundles = bundles; this.giftCards = giftCards; this.categories = categories; this.sink = orderService::log;
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
}
