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

    public OrderService(ProductRepo products, VariantRepo variants, PromotionRepo promos, OrderRepo orders, FlashSaleRepo flash,
                        UserRepo users, PointRepo points, NotifyService notify, StockRepo stock) {
        this.products = products; this.variants = variants; this.promos = promos; this.orders = orders; this.flash = flash;
        this.users = users; this.points = points; this.notify = notify; this.stock = stock;
    }

    public record ItemReq(Long productId, Long variantId, int quantity) {}
    public record CheckoutReq(String buyerName, String phone, String email, String shippingAddress, String city, String note,
                              String promoCode, Integer usePoints, Enums.PaymentMethod paymentMethod, Integer installmentMonths, List<ItemReq> items,
                              Boolean giftWrap, String giftMessage) {}

    /** Phí vận chuyển theo khu vực. */
    public static long shippingFee(String city, long amount) {
        if (amount >= FREE_SHIP_FROM) return 0;
        if (city == null || city.isBlank()) return 35_000;
        if (INNER.contains(city)) return 20_000;
        if (NEAR.contains(city)) return 30_000;
        return 40_000;
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
        return flash.findByProductId(productId).stream().filter(FlashSale::isLive)
                .filter(f -> f.variantId == null || f.variantId.equals(variantId)).findFirst();
    }

    public record Quote(long subtotal, long discount, long pointsDiscount, long shippingFee, long total, int pointsUsed, boolean freeShipVoucher) {}

    @Transactional
    public CustomerOrder place(User user, CheckoutReq r) {
        if (r.items() == null || r.items().isEmpty()) throw ApiException.bad("Giỏ hàng trống");
        if (isBlank(r.buyerName()) || isBlank(r.phone()) || isBlank(r.shippingAddress()))
            throw ApiException.bad("Vui lòng nhập đủ họ tên, số điện thoại và địa chỉ nhận hàng");
        if (r.paymentMethod() == null) throw ApiException.bad("Vui lòng chọn phương thức thanh toán");
        User u = users.findById(user.id).orElseThrow();

        CustomerOrder o = new CustomerOrder();
        o.user = u; o.buyerName = r.buyerName(); o.phone = r.phone(); o.email = r.email(); o.city = r.city();
        o.shippingAddress = r.shippingAddress() + (isBlank(r.city()) || r.shippingAddress().contains(r.city()) ? "" : ", " + r.city());
        o.note = r.note(); o.paymentMethod = r.paymentMethod();
        for (ItemReq it : r.items()) {
            if (it.quantity() <= 0) throw ApiException.bad("Số lượng không hợp lệ");
            Product p = products.findById(it.productId()).filter(x -> x.active).orElseThrow(() -> ApiException.bad("Sản phẩm không tồn tại"));
            OrderItem oi = new OrderItem();
            oi.order = o; oi.productId = p.id; oi.productName = p.name; oi.sku = p.sku; oi.warrantyMonths = p.warrantyMonths;
            oi.image = p.images.isEmpty() ? null : p.images.get(0);
            long unit = p.price;
            ProductVariant v = null;
            if (p.hasVariants) {
                if (it.variantId() == null) throw ApiException.bad("Vui lòng chọn phiên bản cho \"" + p.name + "\"");
                v = variants.findById(it.variantId()).filter(x -> x.product.id.equals(p.id) && x.active)
                        .orElseThrow(() -> ApiException.bad("Phiên bản không tồn tại"));
                if (v.stock < it.quantity()) throw ApiException.bad("\"" + p.name + " – " + v.name + "\" chỉ còn " + v.stock + " sản phẩm");
                unit = v.price; oi.variantId = v.id; oi.variantName = v.name; if (v.image != null) oi.image = v.image;
                v.stock -= it.quantity(); variants.save(v);
                p.stock = Math.max(0, p.stock - it.quantity());
            } else {
                if (p.stock < it.quantity()) throw ApiException.bad("\"" + p.name + "\" chỉ còn " + p.stock + " sản phẩm");
                p.stock -= it.quantity();
            }
            var fs = liveFlash(p.id, v == null ? null : v.id);
            if (fs.isPresent() && fs.get().quota - fs.get().sold >= it.quantity()) {
                unit = fs.get().salePrice; oi.flashSale = true; fs.get().sold += it.quantity(); flash.save(fs.get());
            }
            p.sold += it.quantity();
            log(p, v, -it.quantity(), "Bán hàng", u.email);
            oi.unitPrice = unit; oi.quantity = it.quantity();
            o.items.add(oi);
            o.subtotal += unit * it.quantity();
        }
        boolean freeShip = false;
        if (!isBlank(r.promoCode())) {
            Promotion p = validPromo(r.promoCode(), o.subtotal);
            o.promoCode = p.code; o.discount = discount(p, o.subtotal); freeShip = p.kind == Enums.VoucherKind.FREESHIP;
            p.usedCount++; promos.save(p);
        }
        int use = r.usePoints() == null ? 0 : Math.max(0, Math.min(r.usePoints(), u.points));
        long maxByPoints = (o.subtotal - o.discount) / 2; // dùng điểm tối đa 50% giá trị đơn
        use = (int) Math.min(use, maxByPoints / POINT_VALUE);
        if (use > 0) {
            o.pointsUsed = use; o.pointsDiscount = use * POINT_VALUE; u.points -= use; users.save(u);
            pointTx(u, -use, "Dùng điểm cho đơn hàng");
        }
        long afterDiscount = o.subtotal - o.discount - o.pointsDiscount;
        o.shippingFee = freeShip ? 0 : shippingFee(r.city(), afterDiscount);
        if (Boolean.TRUE.equals(r.giftWrap())) { o.giftWrap = true; o.giftFee = GIFT_FEE; o.giftMessage = r.giftMessage() == null ? null : r.giftMessage().trim().substring(0, Math.min(300, r.giftMessage().trim().length())); }
        o.total = afterDiscount + o.shippingFee + (o.giftFee == null ? 0 : o.giftFee);

        switch (r.paymentMethod()) {
            case INSTALLMENT -> {
                int m = r.installmentMonths() == null ? 6 : r.installmentMonths();
                if (!List.of(3, 6, 12).contains(m)) throw ApiException.bad("Kỳ hạn trả góp 0%: 3, 6 hoặc 12 tháng");
                if (o.total < INSTALLMENT_MIN) throw ApiException.bad("Trả góp 0% qua thẻ tín dụng áp dụng cho đơn từ 3.000.000đ");
                o.installmentMonths = m; o.monthlyInstallment = (long) Math.ceil((double) o.total / m);
            }
            default -> {}
        }
        o.code = "TS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss")) + (int) (Math.random() * 90 + 10);
        o.timeline.put("PENDING", LocalDateTime.now());
        CustomerOrder saved = orders.save(o);
        String how = switch (o.paymentMethod) {
            case COD -> "Thanh toán khi nhận hàng.";
            case BANK_QR -> "Vui lòng chuyển khoản theo mã QR để hoàn tất.";
            case MOMO, VNPAY -> "Vui lòng hoàn tất thanh toán qua cổng " + o.paymentMethod.name() + ".";
            case INSTALLMENT -> "Trả góp 0% " + o.installmentMonths + " tháng qua thẻ tín dụng.";
        };
        notify.send(u, "ORDER", "Đặt hàng thành công", "Đơn " + saved.code + " đã được ghi nhận. " + how, "/orders/" + saved.code);
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
        notify.send(o.user, "ORDER", "Thanh toán thành công", "Đơn " + o.code + " đã nhận " + String.format("%,d", o.paidAmount).replace(',', '.') + "đ.", "/orders/" + o.code);
        return orders.save(o);
    }

    @Transactional
    public CustomerOrder cancel(CustomerOrder o, String reason) {
        if (o.status == Enums.OrderStatus.CANCELLED) return o;
        if (o.status != Enums.OrderStatus.PENDING && o.status != Enums.OrderStatus.CONFIRMED)
            throw ApiException.bad("Không thể huỷ đơn đang giao hoặc đã hoàn tất — vui lòng gửi yêu cầu đổi trả");
        restock(o, "Huỷ đơn " + o.code);
        o.status = Enums.OrderStatus.CANCELLED; o.cancelReason = reason;
        o.timeline.put("CANCELLED", LocalDateTime.now());
        if (o.pointsUsed > 0 && o.user != null) { User u = users.findById(o.user.id).orElseThrow(); u.points += o.pointsUsed; users.save(u); pointTx(u, o.pointsUsed, "Hoàn điểm đơn huỷ " + o.code); }
        notify.send(o.user, "ORDER", "Đơn hàng đã huỷ", "Đơn " + o.code + " đã được huỷ." + (o.paidAmount > 0 ? " Khoản đã thanh toán sẽ được hoàn trong 3–5 ngày làm việc." : ""), "/orders/" + o.code);
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

    public void updateTier(User u) {
        long spent = orders.findByUserIdOrderByCreatedAtDesc(u.id).stream().filter(x -> x.status == Enums.OrderStatus.COMPLETED).mapToLong(x -> x.total).sum();
        u.tier = spent >= 20_000_000 ? "Kim cương" : spent >= 10_000_000 ? "Vàng" : spent >= 3_000_000 ? "Bạc" : "Đồng";
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

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
