package com.ares.shop.service;



import com.ares.shop.model.*;

import com.ares.shop.repo.*;

import com.ares.shop.web.ApiException;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;

import java.util.*;



@Service

public class OrderService {

    public static final long GIFT_FEE = 20_000;

    /** Miễn phí vận chuyển cho đơn từ 500.000đ (sau giảm giá). */

    public static final long FREE_SHIP_FROM = 500_000;

    /** 1 điểm = 1.000đ khi dùng; tích 1 điểm cho mỗi 10.000đ khi đơn hoàn tất. */

    public static final long POINT_VALUE = 1_000, EARN_PER = 10_000;

    public static final long INSTALLMENT_MIN = 3_000_000;

    public static final List<String> INNER = List.of("TP. Hồ Chí Minh", "Hà Nội");

    public static final List<String> NEAR = List.of("Bình Dương", "Đồng Nai", "Long An", "Bà Rịa - Vũng Tàu", "Hải Phòng", "Bắc Ninh", "Hưng Yên", "Cần Thơ", "Đà Nẵng");



    private final ProductRepo products; private final VariantRepo variants; private final PromotionRepo promos; private final OrderRepo orders;

    private final FlashSaleRepo flash; private final UserRepo users; private final PointRepo points; private final NotifyService notify; private final StockRepo stock;

    @org.springframework.beans.factory.annotation.Autowired private BundleRepo bundles;

    @org.springframework.beans.factory.annotation.Autowired private MailService mail;

    @org.springframework.beans.factory.annotation.Autowired private AbandonedCartRepo carts;

    @org.springframework.beans.factory.annotation.Autowired private GiftCardRepo giftCards;

    @org.springframework.beans.factory.annotation.Autowired private ReferralRepo referrals;

    @org.springframework.beans.factory.annotation.Autowired private CarrierShippingService carriers;

    public static final long REFERRAL_DISCOUNT = 50_000; public static final int REFERRAL_REWARD = 100;



    public OrderService(ProductRepo products, VariantRepo variants, PromotionRepo promos, OrderRepo orders, FlashSaleRepo flash,

                        UserRepo users, PointRepo points, NotifyService notify, StockRepo stock) {

        this.products = products; this.variants = variants; this.promos = promos; this.orders = orders; this.flash = flash;

        this.users = users; this.points = points; this.notify = notify; this.stock = stock;

    }



    public record ItemReq(Long productId, Long variantId, int quantity, Long unitPrice) {

        public ItemReq(Long productId, Long variantId, int quantity) {

            this(productId, variantId, quantity, null);

        }

    }

    public record CheckoutReq(String buyerName, String phone, String email, String shippingAddress, String city, String note,

                              String promoCode, Integer usePoints, Enums.PaymentMethod paymentMethod, Integer installmentMonths, List<ItemReq> items,

                              Boolean giftWrap, String giftMessage, String shippingMethod, String giftCardCode, String referralCode,

                              Boolean guest, Boolean autoBestPromo, Boolean useCredit) {}



    /** Phí vận chuyển theo khu vực. */

    public static long shippingFee(String city, long amount) { return shippingFee(city, amount, "STANDARD"); }

    public static long shippingFee(String city, long amount, String method) {

        boolean express = method != null && method.equalsIgnoreCase("EXPRESS");

        if (!express && amount >= FREE_SHIP_FROM) return 0;

        long base;

        if (city == null || city.isBlank()) base = 35_000;

        else if (INNER.contains(city)) base = 20_000;

        else if (NEAR.contains(city)) base = 30_000;

        else base = 40_000;

        return express ? base + 25_000 : base;

    }

    public static java.time.LocalDate[] eta(String city, String method) {

        boolean express = method != null && method.equalsIgnoreCase("EXPRESS");

        int lo = express ? 1 : (INNER.contains(city == null ? "" : city) ? 1 : NEAR.contains(city == null ? "" : city) ? 2 : 3);

        int hi = express ? lo + 1 : lo + 2;

        var from = java.time.LocalDate.now().plusDays(lo);

        return new java.time.LocalDate[]{ from, from.plusDays(hi - lo) };

    }



    public Promotion validPromo(String code, long subtotal) {

        Promotion p = promos.findByCodeIgnoreCase(code == null ? "" : code.trim())

                .filter(Promotion::isValidNow).orElseThrow(() -> ApiException.bad("Mã giảm giá không hợp lệ hoặc đã hết lượt"));

        if (subtotal < p.minOrder) throw ApiException.bad("Đơn tối thiểu " + String.format("%,d", p.minOrder).replace(',', '.') + "đ để dùng mã này");

        return p;

    }



    public long discount(Promotion p, long subtotal) {

        long d = switch (p.kind) {

            case FIXED -> p.amount;

            case FREESHIP -> 0;

            default -> subtotal * p.discountPercent / 100;

        };

        d = p.maxDiscount > 0 ? Math.min(d, p.maxDiscount) : d;

        return Math.min(d, subtotal);

    }



    /** Giá hiện hành (ưu tiên flash sale đang chạy). */

    public Optional<FlashSale> liveFlash(Long productId, Long variantId) {

        if (productId == null) return Optional.empty();

        Product p = products.findById(productId).orElse(null);

        if (p == null) return Optional.empty();

        return flash.findByProductId(productId).stream().filter(FlashSale::isLive)

                .filter(f -> {

                    if (f.variantId != null) return f.variantId.equals(variantId);

                    if (variantId == null) return true;

                    ProductVariant v = variants.findById(variantId).orElse(null);

                    return v == null || v.price == p.price;

                }).findFirst();

    }



    public record Quote(long subtotal, long discount, long pointsDiscount, long shippingFee, long total, int pointsUsed, boolean freeShipVoucher) {}



    @Transactional

    public CustomerOrder place(User user, CheckoutReq r) {

        if (r.items() == null || r.items().isEmpty()) throw ApiException.bad("Giỏ hàng trống");

        if (isBlank(r.buyerName()) || isBlank(r.phone()) || isBlank(r.shippingAddress()))

            throw ApiException.bad("Vui lòng nhập đủ họ tên, số điện thoại và địa chỉ nhận hàng");

        if (r.paymentMethod() == null) throw ApiException.bad("Vui lòng chọn phương thức thanh toán");

        boolean isGuest = user == null || Boolean.TRUE.equals(r.guest());

        if (isGuest && isBlank(r.email())) throw ApiException.bad("Khách vãng lai cần nhập email để nhận hoá đơn");

        User u = isGuest ? null : users.findById(user.id).orElseThrow();



        CustomerOrder o = new CustomerOrder();

        o.user = u; o.guest = isGuest; o.buyerName = r.buyerName(); o.phone = r.phone(); o.email = r.email(); o.city = r.city();

        o.shippingMethod = isBlank(r.shippingMethod()) ? "STANDARD" : r.shippingMethod().toUpperCase();

        o.shippingAddress = r.shippingAddress() + (isBlank(r.city()) || r.shippingAddress().contains(r.city()) ? "" : ", " + r.city());

        o.note = r.note(); o.paymentMethod = r.paymentMethod();

        for (ItemReq it : r.items()) {

            if (it.quantity() <= 0) throw ApiException.bad("Số lượng đặt hàng không hợp lệ");

            Product p = products.findById(it.productId())
                    .orElseThrow(() -> ApiException.bad("Sản phẩm #" + it.productId() + " không tồn tại trong hệ thống"));
            if (!p.active) {
                throw ApiException.bad("Sản phẩm \"" + p.name + "\" hiện đã tạm ẩn hoặc ngừng kinh doanh. Vui lòng xoá khỏi giỏ hàng!");
            }

            OrderItem oi = new OrderItem();
            oi.order = o; oi.productId = p.id; oi.productName = p.name; oi.sku = p.sku; oi.warrantyMonths = p.warrantyMonths;
            oi.image = p.images.isEmpty() ? null : p.images.get(0);
            long unit = p.price;
            ProductVariant v = null;

            if (p.hasVariants) {
                if (it.variantId() == null) {
                    v = variants.findByProductIdOrderByIdAsc(p.id).stream()
                            .filter(x -> x.active && x.stock >= it.quantity())
                            .findFirst()
                            .or(() -> variants.findByProductIdOrderByIdAsc(p.id).stream().filter(x -> x.active && x.stock > 0).findFirst())
                            .or(() -> variants.findByProductIdOrderByIdAsc(p.id).stream().filter(x -> x.active).findFirst())
                            .orElse(null);
                    if (v == null) {
                        throw ApiException.bad("Sản phẩm \"" + p.name + "\" hiện chưa có phiên bản khả dụng hoặc đã tạm ẩn");
                    }
                } else {
                    v = variants.findById(it.variantId()).filter(x -> x.product.id.equals(p.id))
                            .orElseThrow(() -> ApiException.bad("Phiên bản đã chọn của \"" + p.name + "\" không tồn tại"));
                    if (!v.active) {
                        throw ApiException.bad("Phiên bản \"" + v.name + "\" của sản phẩm \"" + p.name + "\" hiện đã tạm ẩn hoặc ngừng kinh doanh");
                    }
                }

                if (v.stock <= 0) {
                    throw ApiException.bad("Sản phẩm \"" + p.name + " (" + v.name + ")\" đã HẾT HÀNG! Vui lòng chọn phiên bản khác hoặc loại bỏ khỏi giỏ hàng.");
                }
                if (v.stock < it.quantity()) {
                    throw ApiException.bad("Sản phẩm \"" + p.name + " (" + v.name + ")\" chỉ còn " + v.stock + " sản phẩm trong kho (bạn đang đặt " + it.quantity() + "). Vui lòng giảm số lượng!");
                }

                unit = v.price; oi.variantId = v.id; oi.variantName = v.name; if (v.image != null) oi.image = v.image;
                v.stock -= it.quantity(); variants.save(v);
                p.stock = Math.max(0, p.stock - it.quantity());
            } else {
                if (p.stock <= 0) {
                    throw ApiException.bad("Sản phẩm \"" + p.name + "\" đã HẾT HÀNG! Vui lòng xoá sản phẩm này khỏi giỏ hàng để tiếp tục thanh toán.");
                }
                if (p.stock < it.quantity()) {
                    throw ApiException.bad("Sản phẩm \"" + p.name + "\" chỉ còn " + p.stock + " sản phẩm trong kho (bạn đang đặt " + it.quantity() + "). Vui lòng giảm số lượng!");
                }
                p.stock -= it.quantity();
            }

            var fs = liveFlash(p.id, v == null ? null : v.id);

            if (fs.isPresent() && fs.get().quota - fs.get().sold >= it.quantity()) {

                unit = fs.get().salePrice; oi.flashSale = true; fs.get().sold += it.quantity(); flash.save(fs.get());

            }

            p.sold += it.quantity();

            products.save(p);

            log(p, v, -it.quantity(), "Bán hàng", u == null ? "guest:" + r.email() : u.email);

            oi.unitPrice = unit; oi.quantity = it.quantity();

            o.items.add(oi);

            o.subtotal += unit * it.quantity();

        }

        o.bundleDiscount = bundleDiscount(o.items);

        if (o.bundleDiscount == 0) o.bundleDiscount = null;

        boolean freeShip = false;

        String promoCode = r.promoCode();

        if (Boolean.TRUE.equals(r.autoBestPromo()) && isBlank(promoCode)) {

            long best = 0; Promotion bestP = null; boolean bestFs = false;

            for (Promotion p : promos.findByActiveTrue()) {

                if (!p.isValidNow() || !p.isPublic || o.subtotal < p.minOrder) continue;

                if (p.kind == Enums.VoucherKind.FREESHIP) { if (!bestFs && best == 0) { bestP = p; bestFs = true; } continue; }

                long d = discount(p, o.subtotal); if (d > best) { best = d; bestP = p; bestFs = false; }

            }

            if (bestP != null) promoCode = bestP.code;

        }

        if (!isBlank(promoCode)) {

            Promotion p = validPromo(promoCode, o.subtotal);

            o.promoCode = p.code; o.discount = discount(p, o.subtotal); freeShip = p.kind == Enums.VoucherKind.FREESHIP;

            p.usedCount++; promos.save(p);

        }

        long bd = o.bundleDiscount == null ? 0 : o.bundleDiscount;

        o.discount = Math.min(o.discount + bd, o.subtotal);

        // Mã giới thiệu: giảm 50.000đ cho đơn đầu tiên của tài khoản mới; người giới thiệu nhận 100 điểm

        if (u != null && !isBlank(r.referralCode())) {

            Referral ref = referrals.findByCodeIgnoreCase(r.referralCode().trim()).orElseThrow(() -> ApiException.bad("Mã giới thiệu không tồn tại"));

            if (ref.ownerUserId.equals(u.id)) throw ApiException.bad("Không thể dùng mã giới thiệu của chính bạn");

            if (!orders.findByUserIdOrderByCreatedAtDesc(u.id).isEmpty()) throw ApiException.bad("Mã giới thiệu chỉ áp dụng cho đơn đầu tiên");

            if (ref.uses >= ref.maxUses) throw ApiException.bad("Mã giới thiệu đã hết lượt");

            o.discount = Math.min(o.discount + REFERRAL_DISCOUNT, o.subtotal); ref.uses++; referrals.save(ref);

            u.referredBy = ref.ownerUserId;

            users.findById(ref.ownerUserId).ifPresent(owner -> { owner.points += REFERRAL_REWARD; users.save(owner); pointTx(owner, REFERRAL_REWARD, "Thưởng giới thiệu bạn mới");

                notify.send(owner, "SYSTEM", "Bạn nhận " + REFERRAL_REWARD + " điểm giới thiệu", "Một người bạn vừa đặt đơn đầu tiên bằng mã của bạn.", "/account?tab=points"); });

            o.note = (o.note == null || o.note.isBlank() ? "" : o.note + " · ") + "Mã giới thiệu " + ref.code.toUpperCase();

        }

        int availPts = u == null ? 0 : u.points;

        int use = r.usePoints() == null ? 0 : Math.max(0, Math.min(r.usePoints(), availPts));

        long maxByPoints = (o.subtotal - o.discount) / 2; // dùng điểm tối đa 50% giá trị đơn

        use = (int) Math.min(use, maxByPoints / POINT_VALUE);

        if (use > 0 && u != null) {

            o.pointsUsed = use; o.pointsDiscount = use * POINT_VALUE; u.points -= use; users.save(u);

            pointTx(u, -use, "Dùng điểm cho đơn hàng");

        }

        long afterDiscount = o.subtotal - o.discount - o.pointsDiscount;

        // Tính phụ phí bảo quản an toàn cho sản phẩm dễ cháy nổ / Pin LiPo

        long hazFee = 0;

        boolean hasHazard = false;

        for (OrderItem oi : o.items) {

            Product p = products.findById(oi.productId).orElse(null);

            if (p != null && p.isHazardous()) {

                hasHazard = true;

                long f = p.getEffectiveHazardousFee();

                if (f > hazFee) hazFee = f;

            }

        }

        if (hasHazard) {

            o.hasHazardous = true;

            o.hazardousFee = hazFee > 0 ? hazFee : 25_000L;

            o.hazardousNote = "Kiện hàng có chứa sản phẩm dễ cháy nổ (Pin LiPo / Hàng nguy hiểm). Đóng gói chống va đập & bao bì an toàn.";

        } else {

            o.hasHazardous = false;

            o.hazardousFee = 0L;

        }



        // store credit / gift card applied later by controller; shipping

        if (carriers != null) {

            var quoteRes = carriers.calculateQuotes(r.city(), null, null, r.shippingAddress(), afterDiscount, 800, hasHazard);

            o.shippingDistanceKm = quoteRes.distanceKm();

            var matchingQuote = quoteRes.quotes().stream()

                    .filter(q -> q.carrierId().equalsIgnoreCase(o.shippingMethod) || (o.shippingMethod != null && o.shippingMethod.toUpperCase().contains(q.carrierId()))).findFirst();

            if (matchingQuote.isPresent()) {

                var q = matchingQuote.get();

                o.carrierName = q.carrierName();

                o.shippingFee = freeShip && !"EXPRESS".equals(q.serviceType()) && !"INSTANT".equals(q.serviceType()) ? 0 : q.finalFee();

                o.etaFrom = q.etaFrom();

                o.etaTo = q.etaTo();

            } else {

                boolean express = "EXPRESS".equalsIgnoreCase(o.shippingMethod);

                o.carrierName = express ? "Giao Hàng Nhanh (GHN Express)" : "Giao Hàng Nhanh (GHN)";

                long rawFee = express ? carriers.computeGhnExpress(quoteRes.distanceKm()) : carriers.computeGhnStandard(quoteRes.distanceKm());

                o.shippingFee = freeShip && !express ? 0 : (freeShip ? Math.max(0, rawFee - 30_000) : rawFee);

                var et = carriers.computeEta(quoteRes.distanceKm(), o.shippingMethod);

                o.etaFrom = et[0];

                o.etaTo = et[1];

            }

        } else {

            o.shippingFee = freeShip && !"EXPRESS".equals(o.shippingMethod) ? 0 : shippingFee(r.city(), afterDiscount, o.shippingMethod);

            var et = eta(r.city(), o.shippingMethod);

            o.etaFrom = et[0];

            o.etaTo = et[1];

        }



        if (Boolean.TRUE.equals(r.giftWrap())) { o.giftWrap = true; o.giftFee = GIFT_FEE; o.giftMessage = r.giftMessage() == null ? null : r.giftMessage().trim().substring(0, Math.min(300, r.giftMessage().trim().length())); }

        o.total = afterDiscount + o.shippingFee + (o.giftFee == null ? 0 : o.giftFee) + (o.hazardousFee == null ? 0 : o.hazardousFee);

        long credit = 0;

        if (!isBlank(r.giftCardCode())) {

            GiftCard g = giftCards.findByCodeIgnoreCase(r.giftCardCode().trim()).filter(x -> x.active && x.balance > 0 && (x.expiresAt == null || x.expiresAt.isAfter(LocalDateTime.now())))

                    .orElseThrow(() -> ApiException.bad("Thẻ quà tặng không hợp lệ hoặc đã hết số dư"));

            long c = Math.min(g.balance, o.total); g.balance -= c; giftCards.save(g); credit += c;

        }

        if (u != null && Boolean.TRUE.equals(r.useCredit()) && u.storeCredit != null && u.storeCredit > 0) {

            long c = Math.min(u.storeCredit, o.total - credit); u.storeCredit -= c; users.save(u); credit += c;

        }

        if (credit > 0) { o.creditUsed = credit; o.total -= credit; }



        switch (r.paymentMethod()) {

            case INSTALLMENT -> {

                int m = r.installmentMonths() == null ? 6 : r.installmentMonths();

                if (!List.of(3, 6, 12).contains(m)) throw ApiException.bad("Kỳ hạn trả góp 0%: 3, 6 hoặc 12 tháng");

                if (o.total < INSTALLMENT_MIN) throw ApiException.bad("Trả góp 0% qua thẻ tín dụng áp dụng cho đơn từ 3.000.000đ");

                o.installmentMonths = m; o.monthlyInstallment = (long) Math.ceil((double) o.total / m);

            }

            default -> {}

        }

        if (o.total == 0) { o.paymentStatus = Enums.PaymentStatus.PAID; o.paidAt = LocalDateTime.now(); }

        o.code = "TS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss")) + (int) (Math.random() * 90 + 10);

        o.timeline.put("PENDING", LocalDateTime.now());

        CustomerOrder saved = orders.save(o);

        String how = switch (o.paymentMethod) {

            case COD -> "Thanh toán khi nhận hàng.";

            case BANK_QR -> "Vui lòng chuyển khoản theo mã QR để hoàn tất.";

            case MOMO, VNPAY -> "Vui lòng hoàn tất thanh toán qua cổng " + o.paymentMethod.name() + ".";

            case INSTALLMENT -> "Trả góp 0% " + o.installmentMonths + " tháng qua thẻ tín dụng.";

        };

        try { mail.invoice(saved); } catch (Exception ignored) {}

        if (u != null) carts.findByUserId(u.id).ifPresent(carts::delete); else if (o.email != null) carts.findByEmailIgnoreCase(o.email).ifPresent(carts::delete);

        if (u != null) notify.send(u, "ORDER", "Đặt hàng thành công", "Đơn " + saved.code + " đã được ghi nhận. " + how, "/orders/" + saved.code);

        return saved;

    }



    /** Thanh toán (giả lập): chuyển khoản QR / ví MoMo / VNPay / trả góp kỳ đầu. */

    @Transactional

    public CustomerOrder pay(CustomerOrder o) {

        if (o.status == Enums.OrderStatus.CANCELLED) throw ApiException.bad("Đơn hàng đã huỷ");

        if (o.paymentStatus == Enums.PaymentStatus.PAID) return o;

        if (o.paymentMethod == Enums.PaymentMethod.INSTALLMENT && o.paidAmount + o.monthlyInstallment < o.total) {

            o.paidAmount += o.monthlyInstallment; o.paymentStatus = Enums.PaymentStatus.PARTIAL;

        } else { o.paidAmount = o.total; o.paymentStatus = Enums.PaymentStatus.PAID; o.paidAt = LocalDateTime.now(); }

        o.transactionRef = (o.paymentMethod == null ? "PAY" : o.paymentMethod.name()) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Tích điểm thưởng & tự động thăng hạng thẻ ngay khi đơn hàng thanh toán thành công
        if (o.paymentStatus == Enums.PaymentStatus.PAID && o.pointsEarned == 0 && o.user != null) {
            int earned = (int) (o.total / EARN_PER);
            if (earned > 0) {
                User u = users.findById(o.user.id).orElse(null);
                if (u != null) {
                    u.points += earned;
                    o.pointsEarned = earned;
                    pointTx(u, earned, "Tích điểm đơn " + o.code);
                    updateTier(u);
                    users.save(u);
                }
            }
        }

        notify.send(o.user, "ORDER", "Thanh toán thành công", "Đơn " + o.code + " đã nhận " + String.format("%,d", o.paidAmount).replace(',', '.') + "đ.", "/orders/" + o.code);

        return orders.save(o);

    }



    @Transactional
    public CustomerOrder cancel(CustomerOrder o, String reason) {
        if (o.status == Enums.OrderStatus.CANCELLED) return o;
        if (o.status != Enums.OrderStatus.PENDING && o.status != Enums.OrderStatus.CONFIRMED && o.status != Enums.OrderStatus.SHIPPING)
            throw ApiException.bad("Không thể huỷ đơn đã hoàn tất — vui lòng gửi yêu cầu đổi trả");
        restock(o, "Huỷ đơn " + o.code);
        o.status = Enums.OrderStatus.CANCELLED;
        o.cancelReason = reason;
        o.timeline.put("CANCELLED", LocalDateTime.now());

        // Tự động kích hoạt hoàn tiền khi khách hàng huỷ đơn đã thanh toán (MoMo, Chuyển khoản, Thẻ)
        if (o.paymentStatus == Enums.PaymentStatus.PAID || o.paidAmount > 0) {
            o.paymentStatus = Enums.PaymentStatus.REFUNDED;
            o.refundedAmount = o.paidAmount > 0 ? o.paidAmount : o.total;
            o.timeline.put("REFUNDED", LocalDateTime.now());
            String methodStr = o.paymentMethod == null ? "MOMO" : o.paymentMethod.name();
            o.transactionRef = (o.transactionRef != null ? o.transactionRef + " | " : "") + "REFUND-" + methodStr + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            if (o.user != null) {
                notify.send(o.user, "ORDER", "Hoàn tiền đơn hàng " + o.code,
                    "Đơn " + o.code + " đã huỷ. Hệ thống đã hoàn tiền " + String.format("%,d", o.refundedAmount).replace(',', '.') + "đ qua " + methodStr + " về tài khoản của bạn.",
                    "/orders/" + o.code);
            }
        }

        if (o.pointsUsed > 0 && o.user != null) {
            User u = users.findById(o.user.id).orElseThrow();
            u.points += o.pointsUsed;
            users.save(u);
            pointTx(u, o.pointsUsed, "Hoàn điểm đơn huỷ " + o.code);
        }
        notify.send(o.user, "ORDER", "Đơn hàng đã huỷ", "Đơn " + o.code + " đã được huỷ thành công." + (o.paidAmount > 0 ? " Toàn bộ số tiền đã được hoàn trả thành công." : ""), "/orders/" + o.code);
        return orders.save(o);
    }



    public void restock(CustomerOrder o, String reason) {

        for (OrderItem it : o.items) if (it.productId != null) products.findById(it.productId).ifPresent(p -> {

            p.stock += it.quantity; p.sold = Math.max(0, p.sold - it.quantity);

            ProductVariant v = it.variantId == null ? null : variants.findById(it.variantId).orElse(null);

            if (v != null) { v.stock += it.quantity; variants.save(v); }

            products.save(p); log(p, v, it.quantity, reason, "system");

        });

    }



    /** Khi đơn hoàn tất: COD coi như đã thu tiền, cộng điểm thành viên. */

    public void complete(CustomerOrder o) {

        if (o.paymentMethod == Enums.PaymentMethod.COD || o.paymentStatus != Enums.PaymentStatus.PAID) {

            o.paidAmount = o.total; o.paymentStatus = Enums.PaymentStatus.PAID; if (o.paidAt == null) o.paidAt = LocalDateTime.now();

        }

        if (o.pointsEarned == 0 && o.user != null) {

            int pts = (int) (o.total / EARN_PER);

            User u = users.findById(o.user.id).orElseThrow();

            u.points += pts; o.pointsEarned = pts; updateTier(u); users.save(u);

            pointTx(u, pts, "Tích điểm đơn " + o.code);

            notify.send(u, "ORDER", "Bạn nhận được " + pts + " điểm", "Cảm ơn bạn đã mua sắm! Điểm có thể dùng để giảm giá đơn sau.", "/account?tab=points");

        }

    }



    public long getTotalSpent(User u) {
        if (u == null || u.id == null) return 0L;
        return orders.findByUserIdOrderByCreatedAtDesc(u.id).stream()
                .filter(x -> (x.status == Enums.OrderStatus.COMPLETED || x.paymentStatus == Enums.PaymentStatus.PAID)
                        && x.status != Enums.OrderStatus.CANCELLED && x.status != Enums.OrderStatus.RETURNED)
                .mapToLong(x -> x.total).sum();
    }

    public void updateTier(User u) {
        if (u == null || u.id == null) return;
        // Tự động kiểm tra và bù điểm cho các đơn hàng đã thanh toán nhưng chưa được tích điểm
        for (CustomerOrder o : orders.findByUserIdOrderByCreatedAtDesc(u.id)) {
            if ((o.paymentStatus == Enums.PaymentStatus.PAID || o.status == Enums.OrderStatus.COMPLETED)
                    && o.status != Enums.OrderStatus.CANCELLED && o.status != Enums.OrderStatus.RETURNED
                    && o.pointsEarned == 0 && o.total > 0) {
                int earned = (int) (o.total / EARN_PER);
                if (earned > 0) {
                    o.pointsEarned = earned;
                    u.points += earned;
                    orders.save(o);
                    pointTx(u, earned, "Tích điểm đơn " + o.code);
                }
            }
        }

        long spent = getTotalSpent(u);
        int pts = u.points;
        String calcTier = (spent >= 20_000_000 || pts >= 2000) ? "Titan" : (spent >= 10_000_000 || pts >= 1000) ? "Vàng" : (spent >= 3_000_000 || pts >= 300) ? "Bạc" : "Đồng";
        List<String> ranks = List.of("Đồng", "Bạc", "Vàng", "Titan");
        String current = (u.tier == null) ? "Đồng" : (u.tier.equalsIgnoreCase("Kim cương") || u.tier.equalsIgnoreCase("Bạch kim")) ? "Titan" : u.tier;
        int curRank = ranks.indexOf(current);
        int calcRank = ranks.indexOf(calcTier);
        if (calcRank > curRank) {
            u.tier = calcTier;
        } else if (u.tier != null && (u.tier.equalsIgnoreCase("Kim cương") || u.tier.equalsIgnoreCase("Bạch kim"))) {
            u.tier = "Titan";
        }
    }



    public void pointTx(User u, int delta, String reason) { PointTx t = new PointTx(); t.user = u; t.delta = delta; t.reason = reason; points.save(t); }



    public void log(Product p, ProductVariant v, int delta, String reason, String by) {

        StockMovement m = new StockMovement(); m.productId = p.id; m.variantId = v == null ? null : v.id;

        m.productName = p.name + (v == null ? "" : " – " + v.name); m.delta = delta; m.after = v == null ? p.stock : v.stock;

        m.reason = reason; m.byUser = by; stock.save(m);

    }



    /** Đơn cho người thắng phiên đấu giá hàng hiếm. */

    @Transactional

    public CustomerOrder auctionOrder(User winner, String name, long amount, String note) {

        CustomerOrder o = new CustomerOrder();

        o.user = winner; o.buyerName = winner.fullName; o.phone = winner.phone; o.email = winner.email;

        o.shippingAddress = "Cập nhật địa chỉ khi thanh toán"; o.note = note;

        OrderItem oi = new OrderItem(); oi.order = o; oi.productName = name + " (đấu giá)"; oi.unitPrice = amount; oi.quantity = 1;

        o.items.add(oi);

        o.subtotal = amount; o.total = amount; o.paymentMethod = Enums.PaymentMethod.BANK_QR;

        o.code = "AU" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss")) + (int) (Math.random() * 90 + 10);

        o.timeline.put("PENDING", LocalDateTime.now());

        return orders.save(o);

    }



    /** Combo: mua X tặng Y (miễn phí món rẻ nhất trong mỗi nhóm) hoặc giá gói cố định. */

    public long bundleDiscount(List<OrderItem> items) {

        long total = 0;

        for (Bundle b : bundles.findByActiveTrueOrderBySortOrderAsc()) {

            if (b.productIds.isEmpty()) continue;

            if ("FIXED".equals(b.kind)) {

                int sets = Integer.MAX_VALUE; long one = 0;

                for (Long pid : new java.util.LinkedHashSet<>(b.productIds)) {

                    var its = items.stream().filter(i -> pid.equals(i.productId)).toList();

                    int q = its.stream().mapToInt(i -> i.quantity).sum(); sets = Math.min(sets, q);

                    one += its.isEmpty() ? 0 : its.stream().mapToLong(i -> i.unitPrice).min().getAsLong();

                }

                if (sets > 0 && sets != Integer.MAX_VALUE && one > b.bundlePrice) total += (one - b.bundlePrice) * sets;

            } else {

                List<Long> units = new ArrayList<>();

                for (OrderItem i : items) if (b.productIds.contains(i.productId)) for (int k = 0; k < i.quantity; k++) units.add(i.unitPrice);

                units.sort(java.util.Comparator.reverseOrder());

                int group = Math.max(1, b.buyQty + b.getQty);

                for (int g = 0; g + group <= units.size(); g += group) for (int k = 0; k < b.getQty; k++) total += units.get(g + group - 1 - k);

            }

        }

        return total;

    }



    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

}

