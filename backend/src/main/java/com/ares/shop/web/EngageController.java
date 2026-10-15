package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.ares.shop.service.MailService;
import com.ares.shop.service.NotifyService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.stream.*;

/** v12: phần thưởng (điểm danh, vòng quay, sinh nhật, huy hiệu), bộ sưu tập, đặt trước, kho theo chi nhánh, thông báo hàng loạt, dòng thời gian khách. */
@RestController
public class EngageController {
    private final UserRepo users; private final PointRepo points; private final OrderRepo orders; private final ReviewRepo reviews; private final PromotionRepo promos;
    private final LookbookRepo books; private final ProductRepo products; private final PreOrderRepo preorders; private final WarehouseStockRepo wh;
    private final NotifyService notify; private final MailService mail; private final TicketRepo tickets; private final ReturnRepo returns; private final QuestionRepo questions;
    private final SecureRandom rnd = new SecureRandom();

    public EngageController(UserRepo users, PointRepo points, OrderRepo orders, ReviewRepo reviews, PromotionRepo promos, LookbookRepo books, ProductRepo products,
                            PreOrderRepo preorders, WarehouseStockRepo wh, NotifyService notify, MailService mail, TicketRepo tickets, ReturnRepo returns, QuestionRepo questions) {
        this.users = users; this.points = points; this.orders = orders; this.reviews = reviews; this.promos = promos; this.books = books; this.products = products;
        this.preorders = preorders; this.wh = wh; this.notify = notify; this.mail = mail; this.tickets = tickets; this.returns = returns; this.questions = questions;
    }

    // ===== Phần thưởng =====
    public static final int[] CHECKIN = {10, 10, 15, 15, 20, 25, 50}; // chuỗi 7 ngày
    static final List<Object[]> SPIN = List.of(new Object[]{"5 điểm", 5, 30}, new Object[]{"10 điểm", 10, 25}, new Object[]{"20 điểm", 20, 15}, new Object[]{"50 điểm", 50, 5},
            new Object[]{"Mã giảm 5%", -5, 10}, new Object[]{"Freeship 30k", -30, 10}, new Object[]{"Chúc may mắn lần sau", 0, 5});
    static final List<Object[]> TIERS = List.of(new Object[]{"Đồng", 0L, "Tích 1 điểm / 10.000đ"}, new Object[]{"Bạc", 3_000_000L, "+5% điểm, quà sinh nhật x1.5"},
            new Object[]{"Vàng", 10_000_000L, "+10% điểm, freeship mọi đơn ≥ 300k"}, new Object[]{"Kim cương", 20_000_000L, "+20% điểm, hỗ trợ ưu tiên, mời sự kiện ra mắt"});

    private User me() { return users.findById(Current.user().id).orElseThrow(); }
    private void addPoints(User u, int d, String reason) { u.points += d; users.save(u); PointTx t = new PointTx(); t.user = u; t.delta = d; t.reason = reason; points.save(t); }
    private long spent(User u) { return orders.findByUserIdOrderByCreatedAtDesc(u.id).stream().filter(o -> o.status == Enums.OrderStatus.COMPLETED).mapToLong(o -> o.total).sum(); }

    @GetMapping("/api/account/rewards")
    public Map<String, Object> rewards() {
        User u = me(); LocalDate today = LocalDate.now(); long spent = spent(u);
        var my = orders.findByUserIdOrderByCreatedAtDesc(u.id); long revs = reviews.findAllByOrderByCreatedAtDesc().stream().filter(r -> r.user != null && r.user.id.equals(u.id)).count();
        int streak = u.lastCheckin != null && !u.lastCheckin.isBefore(today.minusDays(1)) ? Optional.ofNullable(u.checkinStreak).orElse(0) : 0;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("points", u.points); m.put("tier", u.tier); m.put("spent", spent);
        m.put("tiers", TIERS.stream().map(t -> Map.of("name", t[0], "min", t[1], "perk", t[2])).toList());
        Object[] next = TIERS.stream().filter(t -> (Long) t[1] > spent).findFirst().orElse(null);
        m.put("nextTier", next == null ? null : Map.of("name", next[0], "need", (Long) next[1] - spent));
        m.put("checkedToday", today.equals(u.lastCheckin)); m.put("streak", streak); m.put("checkinRewards", CHECKIN);
        m.put("spunToday", today.equals(u.lastSpin)); m.put("spinPrizes", SPIN.stream().map(s -> s[0]).toList());
        m.put("birthday", u.birthday);
        boolean bdMonth = u.birthday != null && u.birthday.getMonth() == today.getMonth();
        m.put("birthdayMonth", bdMonth); m.put("birthdayClaimed", Objects.equals(u.birthdayYearClaimed, today.getYear()));
        long done = my.stream().filter(o -> o.status == Enums.OrderStatus.COMPLETED).count();
        List<Map<String, Object>> badges = new ArrayList<>();
        badge(badges, "first", "Đơn đầu tiên", "Hoàn tất đơn hàng đầu tiên", done >= 1);
        badge(badges, "collector", "Nhà sưu tầm", "Hoàn tất 5 đơn hàng", done >= 5);
        badge(badges, "critic", "Nhà phê bình", "Viết 3 đánh giá", revs >= 3);
        badge(badges, "streak7", "Chăm chỉ", "Điểm danh 7 ngày liên tiếp", streak >= 7);
        badge(badges, "bigspender", "Tay chơi", "Chi tiêu từ 10 triệu", spent >= 10_000_000);
        badge(badges, "rc", "Dân RC", "Mua một mẫu xe điều khiển", my.stream().anyMatch(o -> o.status == Enums.OrderStatus.COMPLETED && o.items.stream().anyMatch(i -> i.productName != null && i.productName.matches("(?i).*(traxxas|kyosho|tamiya|rc|crawler).*"))));
        badge(badges, "birthday", "Sinh nhật vui vẻ", "Nhận quà sinh nhật", u.birthdayYearClaimed != null);
        m.put("badges", badges);
        return m;
    }
    private static void badge(List<Map<String, Object>> l, String k, String n, String d, boolean got) { l.add(Map.of("key", k, "name", n, "desc", d, "earned", got)); }

    @PostMapping("/api/account/checkin") @Transactional
    public Map<String, Object> checkin() {
        User u = me(); LocalDate today = LocalDate.now();
        if (today.equals(u.lastCheckin)) throw ApiException.bad("Hôm nay bạn đã điểm danh rồi");
        int streak = u.lastCheckin != null && u.lastCheckin.equals(today.minusDays(1)) ? Optional.ofNullable(u.checkinStreak).orElse(0) + 1 : 1;
        int pts = CHECKIN[(streak - 1) % 7]; u.lastCheckin = today; u.checkinStreak = streak; addPoints(u, pts, "Điểm danh ngày " + streak);
        return Map.of("points", pts, "streak", streak, "total", u.points);
    }

    @PostMapping("/api/account/spin") @Transactional
    public Map<String, Object> spin() {
        User u = me(); LocalDate today = LocalDate.now();
        if (today.equals(u.lastSpin)) throw ApiException.bad("Mỗi ngày chỉ quay 1 lần — quay lại vào ngày mai nhé");
        int total = SPIN.stream().mapToInt(s -> (Integer) s[2]).sum(), x = rnd.nextInt(total), idx = 0;
        for (int i = 0; i < SPIN.size(); i++) { x -= (Integer) SPIN.get(i)[2]; if (x < 0) { idx = i; break; } }
        Object[] p = SPIN.get(idx); int v = (Integer) p[1]; u.lastSpin = today; String code = null;
        if (v > 0) addPoints(u, v, "Vòng quay may mắn");
        else { users.save(u); if (v < 0) code = personalPromo(u, v == -5 ? Enums.VoucherKind.PERCENT : Enums.VoucherKind.FIXED, v == -5 ? 5 : 30_000, "SPIN", 7, "Quà vòng quay"); }
        Map<String, Object> r = new LinkedHashMap<>(); r.put("index", idx); r.put("label", p[0]); r.put("points", Math.max(v, 0)); r.put("code", code); return r;
    }
    private String personalPromo(User u, Enums.VoucherKind kind, long value, String prefix, int days, String title) {
        Promotion pr = new Promotion(); pr.code = ShopPlusController.genCode(prefix); pr.title = title + " — " + u.fullName; pr.kind = kind;
        if (kind == Enums.VoucherKind.PERCENT) { pr.discountPercent = (int) value; pr.maxDiscount = 200_000; } else pr.amount = value;
        pr.usageLimit = 1; pr.isPublic = false; pr.startDate = LocalDate.now(); pr.endDate = LocalDate.now().plusDays(days); pr.description = "Mã cá nhân, dùng 1 lần";
        promos.save(pr); return pr.code;
    }

    @PutMapping("/api/account/birthday") @Transactional
    public Map<String, Object> setBirthday(@RequestBody Map<String, String> b) {
        User u = me(); if (u.birthday != null) throw ApiException.bad("Ngày sinh đã lưu — liên hệ hỗ trợ nếu cần sửa");
        LocalDate d; try { d = LocalDate.parse(b.get("birthday")); } catch (Exception e) { throw ApiException.bad("Ngày sinh không hợp lệ"); }
        if (d.isAfter(LocalDate.now()) || d.isBefore(LocalDate.now().minusYears(110))) throw ApiException.bad("Ngày sinh không hợp lệ");
        u.birthday = d; users.save(u); return rewards();
    }
    @PostMapping("/api/account/birthday/claim") @Transactional
    public Map<String, Object> claimBirthday() {
        User u = me(); LocalDate t = LocalDate.now();
        if (u.birthday == null || u.birthday.getMonth() != t.getMonth()) throw ApiException.bad("Quà sinh nhật chỉ nhận trong tháng sinh nhật");
        if (Objects.equals(u.birthdayYearClaimed, t.getYear())) throw ApiException.bad("Bạn đã nhận quà sinh nhật năm nay");
        u.birthdayYearClaimed = t.getYear(); int pts = "Đồng".equals(u.tier) || u.tier == null ? 200 : 300; addPoints(u, pts, "Quà sinh nhật " + t.getYear());
        String code = personalPromo(u, Enums.VoucherKind.PERCENT, 15, "HPBD", 30, "Quà sinh nhật");
        notify.send(u, "SYSTEM", "Chúc mừng sinh nhật! 🎂", "Bạn nhận " + pts + " điểm và mã " + code + " giảm 15% (30 ngày).", "/account?tab=rewards");
        return Map.of("points", pts, "code", code);
    }

    // ===== Bộ sưu tập / lookbook =====
    private Map<String, Object> bookView(Lookbook b, boolean withProducts) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("id", b.id); m.put("slug", b.slug); m.put("title", b.title); m.put("subtitle", b.subtitle);
        m.put("description", b.description); m.put("cover", b.cover); m.put("accent", b.accent); m.put("count", b.productIds.size());
        if (withProducts) { Map<Long, Product> pm = products.findAllById(b.productIds).stream().collect(Collectors.toMap(p -> p.id, p -> p));
            m.put("products", b.productIds.stream().map(pm::get).filter(p -> p != null && p.active).toList()); }
        return m;
    }
    @GetMapping("/api/public/collections") public List<Map<String, Object>> collections() { return books.findAllByOrderBySortOrderAscIdAsc().stream().filter(b -> Boolean.TRUE.equals(b.active)).map(b -> bookView(b, false)).toList(); }
    @GetMapping("/api/public/collections/{slug}") public Map<String, Object> collection(@PathVariable String slug) {
        return bookView(books.findBySlug(slug).filter(b -> Boolean.TRUE.equals(b.active)).orElseThrow(() -> ApiException.notFound("Không tìm thấy bộ sưu tập")), true); }
    @GetMapping("/api/admin/collections") public List<Lookbook> adminBooks() { return books.findAllByOrderBySortOrderAscIdAsc(); }
    @PostMapping("/api/admin/collections") public Lookbook createBook(@RequestBody Lookbook b) { b.id = null; return books.save(checkBook(b)); }
    @PutMapping("/api/admin/collections/{id}") public Lookbook updateBook(@PathVariable Long id, @RequestBody Lookbook b) { b.id = id; return books.save(checkBook(b)); }
    @DeleteMapping("/api/admin/collections/{id}") public void delBook(@PathVariable Long id) { books.deleteById(id); }
    private Lookbook checkBook(Lookbook b) {
        if (b.title == null || b.title.isBlank()) throw ApiException.bad("Nhập tên bộ sưu tập");
        if (b.slug == null || b.slug.isBlank()) b.slug = BotController.norm(b.title).replace(' ', '-');
        b.slug = b.slug.toLowerCase().replaceAll("[^a-z0-9-]", "-");
        books.findBySlug(b.slug).filter(x -> !x.id.equals(b.id)).ifPresent(x -> { throw ApiException.bad("Slug đã tồn tại"); });
        if (b.active == null) b.active = true; if (b.sortOrder == null) b.sortOrder = 0; return b;
    }

    // ===== Đặt trước mẫu sắp ra mắt =====
    @GetMapping("/api/public/upcoming")
    public List<Product> upcoming() { return products.findAll().stream().filter(p -> p.active && Boolean.TRUE.equals(p.preorder)).sorted(Comparator.comparing(p -> Optional.ofNullable(p.releaseDate).orElse(LocalDate.MAX))).toList(); }
    public record PreReq(Long productId, String name, String email, String phone, Integer qty, String note) {}
    @PostMapping("/api/public/preorders") @Transactional
    public Map<String, Object> preorder(@RequestBody PreReq r) {
        Product p = products.findById(Optional.ofNullable(r.productId()).orElse(-1L)).filter(x -> Boolean.TRUE.equals(x.preorder)).orElseThrow(() -> ApiException.bad("Sản phẩm không mở đặt trước"));
        User u = Current.userOrNull();
        String email = u != null ? u.email : r.email(); if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw ApiException.bad("Email không hợp lệ");
        int q = Math.max(1, Math.min(5, Optional.ofNullable(r.qty()).orElse(1)));
        boolean dup = preorders.findByProductIdAndStatus(p.id, "WAITING").stream().anyMatch(x -> email.equalsIgnoreCase(x.email)); if (dup) throw ApiException.bad("Bạn đã đặt trước mẫu này");
        PreOrder o = new PreOrder(); o.productId = p.id; o.productName = p.name; o.userId = u == null ? null : u.id; o.name = u != null ? u.fullName : r.name(); o.email = email;
        o.phone = u != null && u.phone != null ? u.phone : r.phone(); o.qty = q; o.deposit = Optional.ofNullable(p.preorderDeposit).orElse(0L) * q; o.note = r.note(); preorders.save(o);
        mail.send(email, "Xác nhận đặt trước " + p.name, "PREORDER", "Cảm ơn bạn đã đặt trước " + q + " × " + p.name + ".\nDự kiến phát hành: " + (p.releaseDate == null ? "đang cập nhật" : p.releaseDate)
                + (o.deposit > 0 ? "\nTiền cọc (thanh toán khi hàng về): " + MailService.vnd(o.deposit) : "") + "\nChúng tôi sẽ báo ngay khi hàng về.");
        return Map.of("id", o.id, "status", o.status, "releaseDate", String.valueOf(p.releaseDate));
    }
    @GetMapping("/api/account/preorders") public List<PreOrder> myPre() { return preorders.findByUserIdOrderByCreatedAtDesc(Current.user().id); }
    @PostMapping("/api/account/preorders/{id}/cancel") public PreOrder cancelMine(@PathVariable Long id) {
        PreOrder o = preorders.findById(id).filter(x -> Current.user().id.equals(x.userId)).orElseThrow(() -> ApiException.notFound("Không tìm thấy"));
        if (!o.status.equals("WAITING") && !o.status.equals("NOTIFIED")) throw ApiException.bad("Không thể huỷ"); o.status = "CANCELLED"; return preorders.save(o); }
    @GetMapping("/api/admin/preorders") public List<PreOrder> allPre() { return preorders.findAllByOrderByCreatedAtDesc(); }
    @PostMapping("/api/admin/preorders/{id}/status") public PreOrder preStatus(@PathVariable Long id, @RequestBody Map<String, String> b) {
        PreOrder o = preorders.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy")); String s = b.getOrDefault("status", "");
        if (!Set.of("WAITING", "NOTIFIED", "CONVERTED", "CANCELLED").contains(s)) throw ApiException.bad("Trạng thái không hợp lệ"); o.status = s;
        if (s.equals("NOTIFIED")) { o.notifiedAt = LocalDateTime.now(); arrived(o); } return preorders.save(o); }
    /** Báo hàng về cho mọi người đang chờ một sản phẩm. */
    @PostMapping("/api/admin/preorders/notify-product/{productId}") @Transactional
    public Map<String, Object> notifyProduct(@PathVariable Long productId) {
        var list = preorders.findByProductIdAndStatus(productId, "WAITING"); list.forEach(o -> { o.status = "NOTIFIED"; o.notifiedAt = LocalDateTime.now(); arrived(o); preorders.save(o); });
        return Map.of("notified", list.size()); }
    private void arrived(PreOrder o) {
        mail.send(o.email, "Hàng về: " + o.productName, "PREORDER", "Mẫu " + o.productName + " bạn đặt trước đã về kho. Hoàn tất đơn trong 72 giờ để giữ hàng: /products/" + o.productId);
        if (o.userId != null) users.findById(o.userId).ifPresent(u -> notify.send(u, "SYSTEM", "Hàng đặt trước đã về", o.productName + " đã sẵn sàng — giữ hàng 72 giờ.", "/products/" + o.productId)); }
    @PutMapping("/api/admin/products/{id}/preorder") public Product setPre(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy")); p.preorder = Boolean.TRUE.equals(b.get("preorder"));
        Object d = b.get("releaseDate"); p.releaseDate = d == null || d.toString().isBlank() ? null : LocalDate.parse(d.toString());
        Object dep = b.get("deposit"); p.preorderDeposit = dep == null ? null : ((Number) dep).longValue(); return products.save(p); }

    // ===== Kho theo chi nhánh =====
    public static final List<String> WAREHOUSES = List.of("HCM", "HN", "DN");
    /** Đảm bảo phân bổ khớp tổng tồn: kho phụ giữ nguyên (giới hạn bởi tổng), HCM nhận phần còn lại. */
    private Map<String, Integer> alloc(Product p) {
        var rows = wh.findByProductId(p.id); Map<String, WarehouseStock> by = rows.stream().collect(Collectors.toMap(r -> r.warehouse, r -> r, (a, b) -> a));
        if (rows.isEmpty()) { int hn = p.stock * 3 / 10, dn = p.stock / 10; save(p.id, "HN", hn); save(p.id, "DN", dn); save(p.id, "HCM", p.stock - hn - dn); return alloc(p); }
        int hn = Math.min(qty(by, "HN"), p.stock), dn = Math.min(qty(by, "DN"), p.stock - hn), hcm = p.stock - hn - dn;
        if (hn != qty(by, "HN")) save(p.id, "HN", hn); if (dn != qty(by, "DN")) save(p.id, "DN", dn); if (hcm != qty(by, "HCM")) save(p.id, "HCM", hcm);
        Map<String, Integer> m = new LinkedHashMap<>(); m.put("HCM", hcm); m.put("HN", hn); m.put("DN", dn); return m;
    }
    private static int qty(Map<String, WarehouseStock> by, String w) { var r = by.get(w); return r == null ? 0 : r.qty; }
    private void save(Long pid, String w, int q) { var r = wh.findByProductId(pid).stream().filter(x -> x.warehouse.equals(w)).findFirst().orElseGet(() -> { var n = new WarehouseStock(); n.productId = pid; n.warehouse = w; return n; }); r.qty = q; wh.save(r); }
    @GetMapping("/api/admin/warehouses") @Transactional
    public Map<String, Object> warehouses() {
        List<Map<String, Object>> rows = new ArrayList<>(); Map<String, Integer> totals = new LinkedHashMap<>(); WAREHOUSES.forEach(w -> totals.put(w, 0));
        for (Product p : products.findAll()) { if (!p.active) continue; var a = alloc(p); a.forEach((k, v) -> totals.merge(k, v, Integer::sum));
            Map<String, Object> m = new LinkedHashMap<>(); m.put("productId", p.id); m.put("name", p.name); m.put("sku", p.sku); m.put("image", p.images.isEmpty() ? "" : p.images.get(0)); m.put("total", p.stock); m.put("by", a); rows.add(m); }
        return Map.of("warehouses", List.of(Map.of("code", "HCM", "name", "Kho Tân Bình, TP.HCM"), Map.of("code", "HN", "name", "Kho Cầu Giấy, Hà Nội"), Map.of("code", "DN", "name", "Kho Hải Châu, Đà Nẵng")), "totals", totals, "rows", rows);
    }
    public record Transfer(Long productId, String from, String to, int qty) {}
    @PostMapping("/api/admin/warehouses/transfer") @Transactional
    public Map<String, Integer> transfer(@RequestBody Transfer t) {
        if (!WAREHOUSES.contains(t.from()) || !WAREHOUSES.contains(t.to()) || t.from().equals(t.to())) throw ApiException.bad("Kho không hợp lệ");
        Product p = products.findById(t.productId()).orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm")); var a = alloc(p);
        if (t.qty() <= 0 || t.qty() > a.get(t.from())) throw ApiException.bad("Kho " + t.from() + " chỉ còn " + a.get(t.from()));
        save(p.id, t.from(), a.get(t.from()) - t.qty()); save(p.id, t.to(), a.get(t.to()) + t.qty()); return alloc(p);
    }

    // ===== Thông báo hàng loạt =====
    public record Broadcast(String title, String body, String link, String audience) {}
    @PostMapping("/api/admin/notifications/broadcast") @Transactional
    public Map<String, Object> broadcast(@RequestBody Broadcast b) {
        if (b.title() == null || b.title().isBlank() || b.body() == null || b.body().isBlank()) throw ApiException.bad("Nhập tiêu đề và nội dung");
        String aud = Optional.ofNullable(b.audience()).orElse("ALL");
        var targets = users.findAll().stream().filter(u -> u.role == Enums.Role.USER && !u.locked).filter(u -> switch (aud) {
            case "ALL" -> true; case "GOLD" -> "Vàng".equals(u.tier) || "Kim cương".equals(u.tier); case "SILVER" -> "Bạc".equals(u.tier); case "BRONZE" -> u.tier == null || "Đồng".equals(u.tier);
            default -> throw ApiException.bad("Đối tượng không hợp lệ"); }).toList();
        targets.forEach(u -> notify.send(u, "SYSTEM", b.title().trim(), b.body().trim(), b.link() == null || b.link().isBlank() ? null : b.link().trim()));
        return Map.of("sent", targets.size());
    }

    // ===== Dòng thời gian khách hàng =====
    @GetMapping("/api/admin/customers/{id}/timeline")
    public List<Map<String, Object>> timeline(@PathVariable Long id) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy khách"));
        List<Map<String, Object>> ev = new ArrayList<>();
        ev.add(e("ACCOUNT", u.createdAt, "Tạo tài khoản", u.provider, null));
        for (var o : orders.findByUserIdOrderByCreatedAtDesc(id)) ev.add(e("ORDER", o.createdAt, "Đặt đơn " + o.code, MailService.vnd(o.total) + " · " + o.status, "/admin/orders?q=" + o.code));
        for (var r : reviews.findAllByOrderByCreatedAtDesc()) if (r.user != null && r.user.id.equals(id)) ev.add(e("REVIEW", r.createdAt, "Đánh giá " + r.rating + "★ " + r.productName, r.comment, "/admin/reviews"));
        for (var t : tickets.findByUserIdOrderByUpdatedAtDesc(id)) ev.add(e("TICKET", t.createdAt, "Phiếu hỗ trợ " + t.code, t.subject + " · " + t.status, "/admin/tickets"));
        for (var r : returns.findByUserIdOrderByCreatedAtDesc(id)) ev.add(e("RETURN", r.createdAt, "Yêu cầu " + (r.kind == null ? "đổi trả" : r.kind) + " " + r.orderCode, r.reason + " · " + r.status, "/admin/returns"));
        for (var q : questions.findAllByOrderByCreatedAtDesc()) if (q.user != null && q.user.id.equals(id)) ev.add(e("QUESTION", q.createdAt, "Hỏi về " + q.productName, q.question, "/admin/questions"));
        for (var p : points.findByUserIdOrderByCreatedAtDesc(id)) ev.add(e("POINTS", p.createdAt, (p.delta > 0 ? "+" : "") + p.delta + " điểm", p.reason, null));
        for (var p : preorders.findByUserIdOrderByCreatedAtDesc(id)) ev.add(e("PREORDER", p.createdAt, "Đặt trước " + p.productName, p.qty + " sp · " + p.status, "/admin/preorders"));
        ev.sort((a, b) -> ((LocalDateTime) b.get("at")).compareTo((LocalDateTime) a.get("at")));
        return ev.size() > 200 ? ev.subList(0, 200) : ev;
    }
    private static Map<String, Object> e(String kind, LocalDateTime at, String title, String detail, String link) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("kind", kind); m.put("at", at == null ? LocalDateTime.MIN : at); m.put("title", title); m.put("detail", detail); m.put("link", link); return m; }
}
