package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.ares.shop.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/** v14: mua định kỳ, tiếp thị liên kết, đa tiền tệ, Web Push. */
@RestController
public class V14Controller {
    private final SubscriptionRepo subs; private final SubscriptionService subService; private final ProductRepo products; private final VariantRepo variants;
    private final AffiliateRepo affs; private final CommissionRepo coms; private final AffiliateService affService; private final UserRepo users;
    private final SettingRepo settings; private final PushSubRepo pushSubs; private final PushService push; private final boolean sandbox;
    private final ObjectMapper json = new ObjectMapper();
    public V14Controller(SubscriptionRepo subs, SubscriptionService subService, ProductRepo products, VariantRepo variants, AffiliateRepo affs, CommissionRepo coms,
                         AffiliateService affService, UserRepo users, SettingRepo settings, PushSubRepo pushSubs, PushService push,
                         @Value("${app.payments.sandbox.enabled:true}") boolean sandbox) {
        this.subs = subs; this.subService = subService; this.products = products; this.variants = variants; this.affs = affs; this.coms = coms; this.affService = affService;
        this.users = users; this.settings = settings; this.pushSubs = pushSubs; this.push = push; this.sandbox = sandbox;
    }
    static boolean blank(String s) { return s == null || s.isBlank(); }

    // ================= Mua định kỳ =================
    public static final List<Integer> INTERVALS = List.of(7, 14, 30, 60, 90);
    public record SubReq(Long productId, Long variantId, Integer quantity, Integer intervalDays, String buyerName, String phone, String shippingAddress, String city, String paymentMethod) {}

    @GetMapping("/api/public/subscriptions/options")
    public Map<String, Object> subOptions() { return Map.of("intervals", INTERVALS, "discountPct", discountPct(), "paymentMethods", List.of("COD", "BANK_QR")); }
    int discountPct() { return settings.findById("subscription.discountPct").map(s -> { try { return Integer.parseInt(s.v); } catch (Exception e) { return 5; } }).orElse(5); }

    @PostMapping("/api/subscriptions") @Transactional
    public ProductSubscription createSub(@RequestBody SubReq r) {
        User u = Current.user();
        Product p = products.findById(r.productId() == null ? -1 : r.productId()).filter(x -> x.active).orElseThrow(() -> ApiException.bad("Sản phẩm không tồn tại"));
        if (Boolean.TRUE.equals(p.preorder)) throw ApiException.bad("Sản phẩm đặt trước không hỗ trợ mua định kỳ");
        int qty = r.quantity() == null ? 1 : r.quantity(); if (qty < 1 || qty > 10) throw ApiException.bad("Số lượng mỗi kỳ từ 1 đến 10");
        int iv = r.intervalDays() == null ? 30 : r.intervalDays(); if (!INTERVALS.contains(iv)) throw ApiException.bad("Chu kỳ hợp lệ: 7, 14, 30, 60 hoặc 90 ngày");
        if (blank(r.buyerName()) || blank(r.phone()) || blank(r.shippingAddress())) throw ApiException.bad("Vui lòng nhập đủ họ tên, số điện thoại và địa chỉ nhận hàng");
        String pm = blank(r.paymentMethod()) ? "COD" : r.paymentMethod().toUpperCase(); if (!List.of("COD", "BANK_QR").contains(pm)) throw ApiException.bad("Mua định kỳ hỗ trợ COD hoặc chuyển khoản QR");
        long unit = p.price; String name = p.name;
        if (p.hasVariants) {
            if (r.variantId() == null) throw ApiException.bad("Vui lòng chọn phiên bản cho \"" + p.name + "\"");
            var v = variants.findById(r.variantId()).filter(x -> x.product.id.equals(p.id) && x.active).orElseThrow(() -> ApiException.bad("Phiên bản không tồn tại"));
            unit = v.price; name = p.name + " – " + v.name;
        }
        if (subs.findByUserIdOrderByIdDesc(u.id).stream().filter(s -> !"CANCELLED".equals(s.status)).count() >= 10) throw ApiException.bad("Tối đa 10 đăng ký định kỳ đang hoạt động");
        ProductSubscription s = new ProductSubscription();
        s.user = users.findById(u.id).orElseThrow(); s.productId = p.id; s.variantId = p.hasVariants ? r.variantId() : null; s.productName = name; s.unitPrice = unit;
        s.image = p.images.isEmpty() ? null : p.images.get(0); s.quantity = qty; s.intervalDays = iv; s.discountPct = discountPct();
        s.buyerName = r.buyerName().trim(); s.phone = r.phone().trim(); s.shippingAddress = r.shippingAddress().trim(); s.city = r.city();
        s.paymentMethod = Enums.PaymentMethod.valueOf(pm); s.nextRunAt = LocalDateTime.now().plusDays(iv);
        return subs.save(s);
    }
    @GetMapping("/api/subscriptions")
    public List<ProductSubscription> mySubs() { return subs.findByUserIdOrderByIdDesc(Current.user().id); }

    public record SubPatch(String status, Integer intervalDays, Integer quantity, Boolean skip) {}
    @PatchMapping("/api/subscriptions/{id}") @Transactional
    public ProductSubscription patchSub(@PathVariable Long id, @RequestBody SubPatch r) {
        ProductSubscription s = subs.findById(id).filter(x -> x.user != null && x.user.id.equals(Current.user().id)).orElseThrow(() -> ApiException.notFound("Không tìm thấy đăng ký"));
        if ("CANCELLED".equals(s.status)) throw ApiException.bad("Đăng ký đã huỷ");
        if (r.status() != null) {
            String st = r.status().toUpperCase(); if (!List.of("ACTIVE", "PAUSED", "CANCELLED").contains(st)) throw ApiException.bad("Trạng thái không hợp lệ");
            if (st.equals("ACTIVE") && !"ACTIVE".equals(s.status)) { s.failCount = 0; if (s.nextRunAt == null || s.nextRunAt.isBefore(LocalDateTime.now())) s.nextRunAt = LocalDateTime.now().plusDays(1); }
            s.status = st;
        }
        if (r.intervalDays() != null) { if (!INTERVALS.contains(r.intervalDays())) throw ApiException.bad("Chu kỳ hợp lệ: 7, 14, 30, 60 hoặc 90 ngày"); s.intervalDays = r.intervalDays(); }
        if (r.quantity() != null) { if (r.quantity() < 1 || r.quantity() > 10) throw ApiException.bad("Số lượng mỗi kỳ từ 1 đến 10"); s.quantity = r.quantity(); }
        if (Boolean.TRUE.equals(r.skip())) s.nextRunAt = (s.nextRunAt == null ? LocalDateTime.now() : s.nextRunAt).plusDays(s.intervalDays);
        return subs.save(s);
    }
    @GetMapping("/api/admin/subscriptions")
    public List<ProductSubscription> allSubs() { return subs.findAllByOrderByIdDesc(); }
    @PostMapping("/api/admin/subscriptions/run-due")
    public Map<String, Object> runDue() { return subService.runDue(); }
    /** Chạy ngay 1 đăng ký (admin) — bỏ qua hạn, dùng cho xử lý thủ công / kiểm thử. */
    @PostMapping("/api/admin/subscriptions/{id}/run")
    public Map<String, Object> runNow(@PathVariable Long id) {
        ProductSubscription s = subs.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy đăng ký"));
        if (!"ACTIVE".equals(s.status)) throw ApiException.bad("Chỉ chạy được đăng ký đang hoạt động");
        CustomerOrder o = subService.runOne(s);
        var fresh = subs.findById(id).orElse(s);
        return o == null ? Map.of("ok", false, "error", String.valueOf(fresh.lastError), "status", fresh.status) : Map.of("ok", true, "orderCode", o.code, "total", o.total, "nextRunAt", fresh.nextRunAt);
    }
    @PutMapping("/api/admin/subscriptions/settings")
    public Map<String, Object> subSettings(@RequestBody Map<String, Object> r) {
        int pct = r.get("discountPct") instanceof Number n ? n.intValue() : -1; if (pct < 0 || pct > 30) throw ApiException.bad("Giảm giá định kỳ từ 0 đến 30%");
        AppSetting s = new AppSetting(); s.k = "subscription.discountPct"; s.v = String.valueOf(pct); settings.save(s); return subOptions();
    }

    // ================= Tiếp thị liên kết =================
    double defaultPct() { return settings.findById("affiliate.pct").map(s -> { try { return Double.parseDouble(s.v); } catch (Exception e) { return 5.0; } }).orElse(5.0); }
    Map<String, Object> affView(Affiliate a) {
        var list = coms.findByAffiliateIdOrderByIdDesc(a.id);
        Map<String, Long> sum = new LinkedHashMap<>(); for (String st : List.of("PENDING", "APPROVED", "PAID", "VOID")) sum.put(st, list.stream().filter(c -> c.status.equals(st)).mapToLong(c -> c.amount).sum());
        long orders = list.stream().filter(c -> !"VOID".equals(c.status)).count();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("affiliate", a); m.put("link", "/?aff=" + a.code); m.put("orders", orders); m.put("conversion", a.clicks == 0 ? 0 : Math.round(orders * 1000.0 / a.clicks) / 10.0);
        m.put("totals", sum); m.put("commissions", list.size() > 50 ? list.subList(0, 50) : list);
        m.put("userName", a.user == null ? null : a.user.fullName); m.put("email", a.user == null ? null : a.user.email);
        return m;
    }
    @GetMapping("/api/affiliate/me")
    public Map<String, Object> myAff() {
        var a = affs.findByUserId(Current.user().id).orElse(null);
        if (a == null) return Map.of("joined", false, "defaultPct", defaultPct());
        affService.sync(); var m = affView(a); m.put("joined", true); return m;
    }
    @PostMapping("/api/affiliate/join") @Transactional
    public Map<String, Object> joinAff() {
        User u = users.findById(Current.user().id).orElseThrow();
        if (affs.findByUserId(u.id).isPresent()) throw ApiException.bad("Bạn đã tham gia chương trình");
        String base = java.text.Normalizer.normalize(Optional.ofNullable(u.fullName).orElse("ARES"), java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D')
            .replaceAll("[^A-Za-z]", "").toUpperCase(); base = base.isEmpty() ? "ARES" : base.substring(0, Math.min(6, base.length()));
        String code; do { code = base + (100 + new Random().nextInt(900)); } while (affs.findByCodeIgnoreCase(code).isPresent());
        Affiliate a = new Affiliate(); a.user = u; a.code = code; a.commissionPct = defaultPct(); affs.save(a);
        var m = affView(a); m.put("joined", true); return m;
    }
    public record PayoutReq(String payoutInfo) {}
    @PutMapping("/api/affiliate/payout-info") @Transactional
    public Map<String, Object> payoutInfo(@RequestBody PayoutReq r) {
        Affiliate a = affs.findByUserId(Current.user().id).orElseThrow(() -> ApiException.bad("Bạn chưa tham gia chương trình"));
        if (r.payoutInfo() != null && r.payoutInfo().length() > 300) throw ApiException.bad("Thông tin nhận tiền tối đa 300 ký tự");
        a.payoutInfo = r.payoutInfo(); affs.save(a); var m = affView(a); m.put("joined", true); return m;
    }
    /** Ghi nhận lượt nhấp liên kết (frontend gọi 1 lần khi thấy ?aff=). */
    @PostMapping("/api/public/aff/{code}/click") @Transactional
    public Map<String, Object> click(@PathVariable String code) {
        Affiliate a = affs.findByCodeIgnoreCase(code).filter(x -> "ACTIVE".equals(x.status)).orElseThrow(() -> ApiException.notFound("Mã giới thiệu không tồn tại"));
        a.clicks++; affs.save(a); return Map.of("ok", true, "code", a.code, "days", 30);
    }
    @GetMapping("/api/admin/affiliates")
    public List<Map<String, Object>> allAffs() {
        affService.sync();
        return affs.findAll().stream().sorted(Comparator.comparing((Affiliate a) -> a.id).reversed()).map(a -> { var m = affView(a); m.remove("commissions"); return m; }).toList();
    }
    @GetMapping("/api/admin/affiliate-commissions")
    public List<AffiliateCommission> allComs() { affService.sync(); return coms.findAllByOrderByIdDesc(); }
    public record AffPatch(Double commissionPct, String status) {}
    @PatchMapping("/api/admin/affiliates/{id}") @Transactional
    public Map<String, Object> patchAff(@PathVariable Long id, @RequestBody AffPatch r) {
        Affiliate a = affs.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy đối tác"));
        if (r.commissionPct() != null) { if (r.commissionPct() < 0 || r.commissionPct() > 30) throw ApiException.bad("Hoa hồng từ 0 đến 30%"); a.commissionPct = r.commissionPct(); }
        if (r.status() != null) { if (!List.of("ACTIVE", "SUSPENDED").contains(r.status())) throw ApiException.bad("Trạng thái không hợp lệ"); a.status = r.status(); }
        affs.save(a); return affView(a);
    }
    @PostMapping("/api/admin/affiliates/{id}/payout") @Transactional
    public Map<String, Object> payout(@PathVariable Long id) {
        Affiliate a = affs.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy đối tác"));
        affService.sync(); long total = 0; int n = 0;
        for (var c : coms.findByAffiliateIdOrderByIdDesc(a.id)) if ("APPROVED".equals(c.status)) { c.status = "PAID"; c.paidAt = LocalDateTime.now(); coms.save(c); total += c.amount; n++; }
        if (n == 0) throw ApiException.bad("Không có hoa hồng đã duyệt để chi trả");
        return Map.of("paid", total, "count", n);
    }
    @PutMapping("/api/admin/affiliates/settings")
    public Map<String, Object> affSettings(@RequestBody Map<String, Object> r) {
        double pct = r.get("defaultPct") instanceof Number n ? n.doubleValue() : -1; if (pct < 0 || pct > 30) throw ApiException.bad("Hoa hồng từ 0 đến 30%");
        AppSetting s = new AppSetting(); s.k = "affiliate.pct"; s.v = String.valueOf(pct); settings.save(s); return Map.of("defaultPct", pct);
    }

    // ================= Đa tiền tệ (chỉ hiển thị; thanh toán luôn bằng VND) =================
    public static final List<String> CURRENCIES = List.of("VND", "USD");
    @SuppressWarnings("unchecked")
    @GetMapping("/api/public/currency")
    public Map<String, Object> currency() {
        Map<String, Object> rates = new LinkedHashMap<>(); rates.put("VND", 1); rates.put("USD", 25400);
        String at = null;
        var st = settings.findById("currency.rates").orElse(null);
        if (st != null) try { Map<String, Object> m = json.readValue(st.v, Map.class); ((Map<String, Object>) m.getOrDefault("rates", Map.of())).forEach(rates::put); at = (String) m.get("updatedAt"); } catch (Exception ignored) {}
        rates.put("VND", 1);
        return Map.of("base", "VND", "currencies", CURRENCIES, "rates", rates, "updatedAt", at == null ? "" : at, "note", "Giá quy đổi chỉ để tham khảo; thanh toán bằng VND.");
    }
    @PutMapping("/api/admin/currency")
    public Map<String, Object> setCurrency(@RequestBody Map<String, Object> r) throws Exception {
        if (!(r.get("rates") instanceof Map<?, ?> in)) throw ApiException.bad("Thiếu bảng tỷ giá");
        Map<String, Object> rates = new LinkedHashMap<>();
        for (var e : in.entrySet()) {
            String c = String.valueOf(e.getKey()).toUpperCase(); if (!CURRENCIES.contains(c) || c.equals("VND")) continue;
            if (!(e.getValue() instanceof Number n) || n.doubleValue() < 1000 || n.doubleValue() > 100000) throw ApiException.bad("Tỷ giá " + c + " phải trong khoảng 1.000 – 100.000 VND");
            rates.put(c, n.doubleValue());
        }
        if (rates.isEmpty()) throw ApiException.bad("Thiếu bảng tỷ giá");
        AppSetting s = new AppSetting(); s.k = "currency.rates"; s.v = json.writeValueAsString(Map.of("rates", rates, "updatedAt", LocalDateTime.now().withNano(0).toString(), "by", Current.user().email)); settings.save(s);
        return currency();
    }

    // ================= Web Push =================
    @GetMapping("/api/public/push/key")
    public Map<String, Object> pushKey() { return Map.of("publicKey", push.publicKey()); }
    public record PushSubReq(String endpoint, Map<String, String> keys) {}
    @PostMapping("/api/push/subscribe") @Transactional
    public Map<String, Object> subscribe(@RequestBody PushSubReq r) {
        User u = Current.user();
        if (r.endpoint() == null || r.endpoint().length() > 600 || r.keys() == null || blank(r.keys().get("p256dh")) || blank(r.keys().get("auth"))) throw ApiException.bad("Đăng ký push không hợp lệ");
        if (!push.endpointAllowed(r.endpoint())) throw ApiException.bad("Dịch vụ push không được hỗ trợ");
        try { if (Base64.getUrlDecoder().decode(r.keys().get("p256dh")).length != 65 || Base64.getUrlDecoder().decode(r.keys().get("auth")).length != 16) throw new IllegalArgumentException(); }
        catch (Exception e) { throw ApiException.bad("Khoá push không hợp lệ"); }
        PushSub s = pushSubs.findByEndpoint(r.endpoint()).orElseGet(PushSub::new);
        s.endpoint = r.endpoint(); s.userId = u.id; s.p256dh = r.keys().get("p256dh"); s.auth = r.keys().get("auth"); s.failures = 0; pushSubs.save(s);
        return Map.of("ok", true, "id", s.id, "devices", pushSubs.findByUserId(u.id).size());
    }
    public record EndpointReq(String endpoint) {}
    @PostMapping("/api/push/unsubscribe") @Transactional
    public Map<String, Object> unsubscribe(@RequestBody EndpointReq r) {
        Long uid = Current.user().id;
        pushSubs.findByEndpoint(String.valueOf(r.endpoint())).filter(s -> uid.equals(s.userId)).ifPresent(pushSubs::delete);
        return Map.of("ok", true, "devices", pushSubs.findByUserId(uid).size());
    }
    @GetMapping("/api/push/status")
    public Map<String, Object> pushStatus() { return Map.of("devices", pushSubs.findByUserId(Current.user().id).size()); }
    /** Gửi thử tới chính thiết bị của mình. */
    @PostMapping("/api/push/test")
    public Map<String, Object> pushTest() {
        User u = Current.user(); int ok = push.sendToUser(u.id, "Ares Toys", "Thông báo đẩy đã hoạt động trên thiết bị này 🎉", "/account");
        return Map.of("delivered", ok, "devices", pushSubs.findByUserId(u.id).size());
    }
    @GetMapping("/api/admin/push/subscriptions")
    public List<Map<String, Object>> allPush() {
        return pushSubs.findAll().stream().map(s -> { Map<String, Object> m = new LinkedHashMap<>(); m.put("id", s.id); m.put("userId", s.userId);
            m.put("service", java.net.URI.create(s.endpoint).getHost()); m.put("sent", s.sent); m.put("failures", s.failures); m.put("lastStatus", s.lastStatus); m.put("lastSentAt", s.lastSentAt); m.put("createdAt", s.createdAt); return m; }).toList();
    }
    public record BroadcastReq(String title, String body, String url) {}
    @PostMapping("/api/admin/push/broadcast")
    public Map<String, Object> broadcast(@RequestBody BroadcastReq r) {
        if (blank(r.title()) || r.title().length() > 80) throw ApiException.bad("Tiêu đề 1–80 ký tự");
        if (r.body() != null && r.body().length() > 240) throw ApiException.bad("Nội dung tối đa 240 ký tự");
        String url = blank(r.url()) ? "/" : r.url(); if (!url.startsWith("/")) throw ApiException.bad("Liên kết phải là đường dẫn trong shop (bắt đầu bằng /)");
        int ok = 0, fail = 0;
        for (PushSub s : List.copyOf(pushSubs.findAll())) { int c = push.send(s, Map.of("title", r.title(), "body", r.body() == null ? "" : r.body(), "url", url, "tag", "broadcast")); if (c >= 200 && c < 300) ok++; else fail++; }
        return Map.of("delivered", ok, "failed", fail);
    }

    // ---- Hộp thư push sandbox (chỉ khi bật sandbox): nhận bản tin đã mã hoá để kiểm thử end-to-end ----
    private final Map<String, Deque<Map<String, Object>>> inbox = new java.util.concurrent.ConcurrentHashMap<>();
    @PostMapping("/api/public/push/sandbox-inbox/{id}")
    public org.springframework.http.ResponseEntity<Void> inboxPost(@PathVariable String id, @RequestBody(required = false) byte[] body,
                                                                    @RequestHeader Map<String, String> h) {
        if (!sandbox) throw ApiException.notFound("Không tìm thấy");
        if (id.length() > 64 || inbox.size() > 100) throw ApiException.bad("Hộp thư sandbox đầy");
        var q = inbox.computeIfAbsent(id, k -> new ConcurrentLinkedDeque<>());
        Map<String, Object> m = new LinkedHashMap<>(); m.put("at", LocalDateTime.now().toString()); m.put("body", Base64.getEncoder().encodeToString(body == null ? new byte[0] : body));
        m.put("contentEncoding", h.get("content-encoding")); m.put("ttl", h.get("ttl")); m.put("authorization", h.get("authorization"));
        q.addFirst(m); while (q.size() > 20) q.removeLast();
        return org.springframework.http.ResponseEntity.status(HttpStatus.CREATED).build();
    }
    @GetMapping("/api/public/push/sandbox-inbox/{id}")
    public Collection<Map<String, Object>> inboxGet(@PathVariable String id) {
        if (!sandbox) throw ApiException.notFound("Không tìm thấy");
        return inbox.getOrDefault(id, new ConcurrentLinkedDeque<>());
    }
}
