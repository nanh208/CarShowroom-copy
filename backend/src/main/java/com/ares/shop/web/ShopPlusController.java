package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.ares.shop.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;

/** v11 — tính năng khách hàng: khách vãng lai, theo dõi đơn công khai, thẻ quà, giới thiệu, combo, giỏ đã lưu, 2FA… */
@RestController
public class ShopPlusController {
    private final OrderRepo orders; private final ProductRepo products; private final UserRepo users; private final OrderService orderService;
    private final GiftCardRepo giftCards; private final ReferralRepo referrals; private final BundleRepo bundles; private final SavedCartRepo saved;
    private final AbandonedCartRepo abandoned; private final ReviewRepo reviews; private final ReviewVoteRepo votes; private final StockNotifyRepo notifyMe;
    private final MailService mail; private final SettingRepo settings; private final PromotionRepo promos; private final EmailLogRepo emails;
    private final ObjectMapper json = new ObjectMapper();

    public ShopPlusController(OrderRepo orders, ProductRepo products, UserRepo users, OrderService orderService, GiftCardRepo giftCards, ReferralRepo referrals,
                              BundleRepo bundles, SavedCartRepo saved, AbandonedCartRepo abandoned, ReviewRepo reviews, ReviewVoteRepo votes,
                              StockNotifyRepo notifyMe, MailService mail, SettingRepo settings, PromotionRepo promos, EmailLogRepo emails) {
        this.orders = orders; this.products = products; this.users = users; this.orderService = orderService; this.giftCards = giftCards;
        this.referrals = referrals; this.bundles = bundles; this.saved = saved; this.abandoned = abandoned; this.reviews = reviews; this.votes = votes;
        this.notifyMe = notifyMe; this.mail = mail; this.settings = settings; this.promos = promos; this.emails = emails;
    }

    @org.springframework.beans.factory.annotation.Autowired com.ares.shop.service.CarrierService carrierService;
    @org.springframework.beans.factory.annotation.Autowired com.ares.shop.service.AffiliateService affiliates;
    // ===== Khách vãng lai: đặt hàng không cần tài khoản (COD) =====
    @PostMapping("/api/public/checkout")
    public Map<String, Object> guestCheckout(@RequestBody OrderService.CheckoutReq r, @RequestHeader(value = "X-Aff", required = false) String aff) {
        if (r.paymentMethod() != Enums.PaymentMethod.COD) throw ApiException.bad("Khách vãng lai thanh toán khi nhận hàng (COD). Đăng nhập để dùng ví/thẻ.");
        if (r.email() == null || !r.email().contains("@")) throw ApiException.bad("Vui lòng nhập email hợp lệ để nhận hoá đơn");
        CustomerOrder o = orderService.place(null, new OrderService.CheckoutReq(r.buyerName(), r.phone(), r.email(), r.shippingAddress(), r.city(), r.note(),
                r.promoCode(), 0, r.paymentMethod(), null, r.items(), r.giftWrap(), r.giftMessage(), r.shippingMethod(), r.giftCardCode(), null, true, r.autoBestPromo(), false, r.carrier()));
        try { affiliates.attribute(o, aff); } catch (Exception ignored) {}
        return Map.of("code", o.code, "total", o.total, "etaFrom", o.etaFrom, "etaTo", o.etaTo, "email", o.email);
    }

    // ===== Theo dõi đơn công khai theo mã (không lộ thông tin cá nhân) =====
    @GetMapping("/api/public/track/{code}")
    public Map<String, Object> track(@PathVariable String code) {
        CustomerOrder o = orders.findByCode(code.trim().toUpperCase()).orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng " + code));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", o.code); m.put("status", o.status); m.put("paymentStatus", o.paymentStatus); m.put("timeline", o.timeline);
        m.put("createdAt", o.createdAt); m.put("city", o.city); m.put("shippingMethod", Optional.ofNullable(o.shippingMethod).orElse("STANDARD"));
        m.put("etaFrom", o.etaFrom); m.put("etaTo", o.etaTo); m.put("buyer", mask(o.buyerName));
        m.put("items", o.items.stream().map(i -> Map.of("name", i.productName, "qty", i.quantity, "image", Optional.ofNullable(i.image).orElse(""))).toList());
        return m;
    }
    static String mask(String n) { if (n == null || n.isBlank()) return "Khách"; String[] p = n.trim().split("\\s+"); return p[p.length - 1] + " ***"; }

    // ===== "Vừa có người mua" — ẩn danh =====
    @GetMapping("/api/public/recent-purchases")
    public List<Map<String, Object>> recent() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (CustomerOrder o : orders.findAllByOrderByCreatedAtDesc()) {
            if (o.status == Enums.OrderStatus.CANCELLED || o.items.isEmpty() || o.items.get(0).productId == null) continue;
            OrderItem i = o.items.get(0);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("who", mask(o.buyerName)); m.put("city", Optional.ofNullable(o.city).orElse("Việt Nam"));
            m.put("productId", i.productId); m.put("name", i.productName); m.put("image", i.image); m.put("at", o.createdAt);
            out.add(m); if (out.size() >= 12) break;
        }
        return out;
    }

    // ===== Phương thức giao + ngày giao dự kiến =====
    @GetMapping("/api/public/shipping-options")
    public List<Map<String, Object>> shipOptions(@RequestParam(required = false) String city, @RequestParam(defaultValue = "0") long amount) {
        List<Map<String, Object>> l = new ArrayList<>();
        for (String m : List.of("STANDARD", "EXPRESS")) {
            var e = OrderService.eta(city, m);
            l.add(Map.of("method", m, "label", m.equals("EXPRESS") ? "Hoả tốc" : "Tiêu chuẩn", "fee", OrderService.shippingFee(city, amount, m), "etaFrom", e[0], "etaTo", e[1]));
        }
        return l;
    }

    public record QuoteItem(Long productId, int quantity) {}
    public record QuoteReq(String city, long amount, List<QuoteItem> items) {}
    /** Báo giá các hãng (GHN/GHTK mô phỏng) cho giỏ hàng: theo khối lượng ước tính + vùng; freeship tiêu chuẩn từ 500k. */
    @PostMapping("/api/public/shipping-quote")
    public List<Map<String, Object>> shippingQuote(@RequestBody QuoteReq r) {
        int grams = 150 + Optional.ofNullable(r.items()).orElse(List.of()).stream().limit(50)
            .mapToInt(i -> CarrierService.weightOf(i.productId() == null ? null : products.findById(i.productId()).orElse(null)) * Math.max(1, Math.min(99, i.quantity()))).sum();
        List<Map<String, Object>> out = new ArrayList<>();
        for (var q : carrierService.quoteAll(r.city(), grams, 0)) {
            long fee = "STANDARD".equals(q.service()) && r.amount() >= OrderService.FREE_SHIP_FROM ? 0 : q.fee();
            out.add(Map.of("carrier", q.carrier(), "method", q.service(), "label", q.label(), "fee", fee, "listFee", q.fee(), "weight", q.weight(), "etaFrom", q.etaFrom(), "etaTo", q.etaTo()));
        }
        return out;
    }

    // ===== Tự áp mã giảm tốt nhất =====
    @GetMapping("/api/public/best-promo")
    public Map<String, Object> bestPromo(@RequestParam long subtotal) {
        Promotion best = null; long bd = 0;
        for (Promotion p : promos.findByActiveTrue()) {
            if (!p.isValidNow() || !p.isPublic || subtotal < p.minOrder || p.kind == Enums.VoucherKind.FREESHIP) continue;
            long d = orderService.discount(p, subtotal); if (d > bd) { bd = d; best = p; }
        }
        if (best == null) return Map.of("code", "", "discount", 0);
        return Map.of("code", best.code, "discount", bd, "title", Optional.ofNullable(best.title).orElse(best.code));
    }

    // ===== Combo / Bundle =====
    @GetMapping("/api/public/bundles")
    public List<Map<String, Object>> bundleList(@RequestParam(required = false) Long productId) {
        return bundles.findByActiveTrueOrderBySortOrderAsc().stream().filter(b -> productId == null || b.productIds.contains(productId)).map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("bundle", b);
            var ps = b.productIds.stream().distinct().map(id -> products.findById(id).filter(p -> p.active).orElse(null)).filter(Objects::nonNull)
                    .map(p -> Map.of("id", p.id, "name", p.name, "price", p.price, "image", p.images.isEmpty() ? "" : p.images.get(0), "stock", p.stock, "hasVariants", p.hasVariants)).toList();
            m.put("products", ps);
            if ("FIXED".equals(b.kind)) { long sum = ps.stream().mapToLong(x -> (long) x.get("price")).sum(); m.put("sum", sum); m.put("save", Math.max(0, sum - b.bundlePrice)); }
            return m;
        }).toList();
    }
    @PostMapping("/api/public/bundle-quote")
    public Map<String, Object> bundleQuote(@RequestBody List<OrderService.ItemReq> items) {
        List<OrderItem> its = new ArrayList<>();
        for (var r : items) products.findById(r.productId()).ifPresent(p -> {
            OrderItem i = new OrderItem(); i.productId = p.id; i.quantity = Math.max(1, r.quantity());
            long unit = p.price; var f = orderService.liveFlash(p.id, r.variantId()); if (f.isPresent()) unit = f.get().salePrice; i.unitPrice = unit; its.add(i); });
        return Map.of("discount", orderService.bundleDiscount(its));
    }

    // ===== Báo khi có hàng (email) =====
    public record NotifyReq(Long productId, String email) {}
    @PostMapping("/api/public/notify-me")
    public Map<String, Object> notifyMe(@RequestBody NotifyReq r) {
        User me = Current.userOrNull();
        String email = r.email() != null && !r.email().isBlank() ? r.email().trim() : me == null ? null : me.email;
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw ApiException.bad("Email không hợp lệ");
        Product p = products.findById(r.productId()).orElseThrow(() -> ApiException.notFound("Sản phẩm không tồn tại"));
        if (!notifyMe.existsByProductIdAndEmailIgnoreCaseAndSentIsNull(p.id, email)) { StockNotify n = new StockNotify(); n.productId = p.id; n.email = email; notifyMe.save(n); }
        return Map.of("ok", true, "email", email);
    }

    // ===== Danh sách yêu thích chia sẻ =====
    @GetMapping("/api/public/wishlist")
    public List<Product> sharedWishlist(@RequestParam String ids) {
        return Arrays.stream(ids.split(",")).limit(50).map(String::trim).filter(x -> x.matches("\\d+")).map(Long::valueOf)
                .map(id -> products.findById(id).filter(p -> p.active).orElse(null)).filter(Objects::nonNull).toList();
    }

    // ===== Bố cục trang chủ (admin sắp xếp) =====
    public static final List<String> HOME_SECTIONS = List.of("hero", "flash", "bundles", "featured", "auctions", "recent", "reviews");
    @GetMapping("/api/public/home-layout")
    public List<Map<String, Object>> homeLayout() {
        try {
            var s = settings.findById("homeLayout");
            if (s.isPresent()) return json.readValue(s.get().v, json.getTypeFactory().constructCollectionType(List.class, Map.class));
        } catch (Exception ignored) {}
        return HOME_SECTIONS.stream().map(k -> Map.<String, Object>of("key", k, "visible", true)).toList();
    }

    // ===== Thẻ quà tặng & tín dụng cửa hàng =====
    public record BuyGift(long amount, String recipientEmail, String message) {}
    @PostMapping("/api/giftcards/buy") @Transactional
    public Map<String, Object> buyGift(@RequestBody BuyGift r) {
        User me = Current.user();
        if (!List.of(100_000L, 200_000L, 500_000L, 1_000_000L, 2_000_000L).contains(r.amount())) throw ApiException.bad("Mệnh giá: 100K, 200K, 500K, 1 triệu hoặc 2 triệu");
        GiftCard g = new GiftCard(); g.code = genCode("GC"); g.initial = r.amount(); g.balance = r.amount(); g.buyerUserId = me.id;
        g.message = r.message() == null ? null : r.message().substring(0, Math.min(200, r.message().length())); g.expiresAt = LocalDateTime.now().plusYears(1);
        giftCards.save(g);
        String to = r.recipientEmail() == null || r.recipientEmail().isBlank() ? me.email : r.recipientEmail().trim();
        mail.send(to, "Bạn nhận được thẻ quà ARES Toys " + MailService.vnd(g.initial), "GIFT_CARD",
                me.fullName + " gửi tặng bạn thẻ quà trị giá " + MailService.vnd(g.initial) + ".\n" + (g.message == null ? "" : "Lời nhắn: “" + g.message + "”\n") +
                "\nMã thẻ: " + g.code + "\nNhập mã khi thanh toán hoặc nạp vào ví tại Tài khoản › Ví & quà tặng. Hạn dùng 12 tháng.");
        return Map.of("code", g.code, "amount", g.initial, "sentTo", to);
    }
    @GetMapping("/api/public/giftcards/{code}")
    public Map<String, Object> giftBalance(@PathVariable String code) {
        GiftCard g = giftCards.findByCodeIgnoreCase(code.trim()).filter(x -> x.active).orElseThrow(() -> ApiException.notFound("Mã thẻ không tồn tại"));
        return Map.of("code", g.code, "balance", g.balance, "expiresAt", String.valueOf(g.expiresAt));
    }
    @PostMapping("/api/account/giftcards/redeem") @Transactional
    public Map<String, Object> redeem(@RequestBody Map<String, String> b) {
        User u = users.findById(Current.user().id).orElseThrow();
        GiftCard g = giftCards.findByCodeIgnoreCase(Optional.ofNullable(b.get("code")).orElse("").trim())
                .filter(x -> x.active && x.balance > 0 && (x.expiresAt == null || x.expiresAt.isAfter(LocalDateTime.now())))
                .orElseThrow(() -> ApiException.bad("Mã thẻ không hợp lệ, đã dùng hết hoặc hết hạn"));
        long add = g.balance; g.balance = 0; g.redeemedByUserId = u.id; giftCards.save(g);
        u.storeCredit = Optional.ofNullable(u.storeCredit).orElse(0L) + add; users.save(u);
        return Map.of("added", add, "storeCredit", u.storeCredit);
    }
    @GetMapping("/api/account/wallet")
    public Map<String, Object> wallet() {
        User u = users.findById(Current.user().id).orElseThrow();
        Referral ref = referrals.findByOwnerUserId(u.id).orElseGet(() -> { Referral r = new Referral(); r.ownerUserId = u.id;
            String base = Optional.ofNullable(u.fullName).orElse("ARES").replaceAll("[^A-Za-z]", "").toUpperCase();
            r.code = (base.length() > 5 ? base.substring(base.length() - 5) : base) + (100 + new Random().nextInt(900)); return referrals.save(r); });
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("storeCredit", Optional.ofNullable(u.storeCredit).orElse(0L)); m.put("points", u.points); m.put("pointValue", OrderService.POINT_VALUE);
        m.put("referral", Map.of("code", ref.code, "uses", ref.uses, "reward", OrderService.REFERRAL_REWARD, "discount", OrderService.REFERRAL_DISCOUNT));
        m.put("giftCards", giftCards.findByBuyerUserIdOrRedeemedByUserId(u.id, u.id));
        m.put("inbox", emails.findTop50ByToAddrIgnoreCaseOrderByCreatedAtDesc(u.email));
        return m;
    }
    static String genCode(String pre) { String a = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; Random r = new java.security.SecureRandom(); StringBuilder s = new StringBuilder(pre);
        for (int i = 0; i < 10; i++) { if (i == 4 || i == 8) s.append('-'); s.append(a.charAt(r.nextInt(a.length()))); } return s.toString(); }

    // ===== Giỏ đã lưu =====
    public record SaveCart(String name, Object items) {}
    @GetMapping("/api/account/saved-carts") public List<SavedCart> savedCarts() { return saved.findByUserIdOrderByUpdatedAtDesc(Current.user().id); }
    @PostMapping("/api/account/saved-carts")
    public SavedCart saveCart(@RequestBody SaveCart r) throws Exception {
        if (saved.findByUserIdOrderByUpdatedAtDesc(Current.user().id).size() >= 10) throw ApiException.bad("Tối đa 10 giỏ đã lưu");
        SavedCart c = new SavedCart(); c.userId = Current.user().id; c.name = r.name() == null || r.name().isBlank() ? "Giỏ " + LocalDate.now() : r.name().trim();
        c.itemsJson = json.writeValueAsString(r.items()); if (c.itemsJson.length() > 8000) throw ApiException.bad("Giỏ quá lớn");
        return saved.save(c);
    }
    @DeleteMapping("/api/account/saved-carts/{id}")
    public void delCart(@PathVariable Long id) { saved.findById(id).filter(c -> c.userId.equals(Current.user().id)).ifPresent(saved::delete); }

    // ===== Đồng bộ giỏ (để admin thấy giỏ bị bỏ quên) =====
    public record CartSync(Object items, long subtotal, String email) {}
    @PutMapping("/api/public/cart-sync")
    public Map<String, Object> cartSync(@RequestBody CartSync r) throws Exception {
        User me = Current.userOrNull();
        if (me == null && (r.email() == null || r.email().isBlank())) return Map.of("ok", false);
        AbandonedCart c = (me != null ? abandoned.findByUserId(me.id) : abandoned.findByEmailIgnoreCase(r.email())).orElseGet(AbandonedCart::new);
        String items = json.writeValueAsString(r.items());
        if (r.subtotal() <= 0 || "[]".equals(items)) { if (c.id != null) abandoned.delete(c); return Map.of("ok", true); }
        if (items.length() > 8000) return Map.of("ok", false);
        c.userId = me == null ? null : me.id; c.email = me == null ? r.email() : me.email; c.name = me == null ? null : me.fullName;
        c.itemsJson = items; c.subtotal = r.subtotal(); c.lastSeen = LocalDateTime.now(); abandoned.save(c);
        return Map.of("ok", true);
    }

    // ===== Đánh giá hữu ích =====
    @PostMapping("/api/reviews/{id}/helpful") @Transactional
    public Map<String, Object> helpful(@PathVariable Long id) {
        User me = Current.user(); Review r = reviews.findById(id).orElseThrow(() -> ApiException.notFound("Đánh giá không tồn tại"));
        var v = votes.findByReviewIdAndUserId(id, me.id); boolean on;
        if (v.isPresent()) { votes.delete(v.get()); on = false; } else { ReviewVote x = new ReviewVote(); x.reviewId = id; x.userId = me.id; votes.save(x); on = true; }
        votes.flush(); r.helpful = (int) votes.countByReviewIdAndHelpfulTrue(id); reviews.save(r);
        return Map.of("helpful", r.helpful, "voted", on);
    }

    // ===== Gửi lại hoá đơn qua email (mô phỏng) =====
    @PostMapping("/api/orders/{code}/email-invoice")
    public Map<String, Object> emailInvoice(@PathVariable String code) {
        User me = Current.user();
        CustomerOrder o = orders.findByCode(code).filter(x -> (x.user != null && x.user.id.equals(me.id)) || me.role != Enums.Role.USER).orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn"));
        if (o.email == null || o.email.isBlank()) o.email = me.email;
        var e = mail.invoice(o);
        return Map.of("sentTo", e.toAddr, "at", e.createdAt);
    }

    // ===== 2FA (TOTP) tuỳ chọn =====
    @PostMapping("/api/account/2fa/setup")
    public Map<String, Object> setup2fa() {
        User u = users.findById(Current.user().id).orElseThrow();
        if (Boolean.TRUE.equals(u.totpEnabled)) throw ApiException.bad("2FA đã bật");
        u.totpSecret = Totp.newSecret(); users.save(u);
        String uri = "otpauth://totp/ARES%20Toys:" + u.email + "?secret=" + u.totpSecret + "&issuer=ARES%20Toys&digits=6&period=30";
        return Map.of("secret", u.totpSecret, "otpauth", uri);
    }
    @PostMapping("/api/account/2fa/enable")
    public Map<String, Object> enable2fa(@RequestBody Map<String, String> b) {
        User u = users.findById(Current.user().id).orElseThrow();
        if (!Totp.verify(u.totpSecret, b.get("code"))) throw ApiException.bad("Mã xác thực không đúng");
        u.totpEnabled = true; users.save(u); return Map.of("enabled", true);
    }
    @PostMapping("/api/account/2fa/disable")
    public Map<String, Object> disable2fa(@RequestBody Map<String, String> b) {
        User u = users.findById(Current.user().id).orElseThrow();
        if (Boolean.TRUE.equals(u.totpEnabled) && !Totp.verify(u.totpSecret, b.get("code"))) throw ApiException.bad("Mã xác thực không đúng");
        u.totpEnabled = false; u.totpSecret = null; users.save(u); return Map.of("enabled", false);
    }
}
