package com.ares.shop.service;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.ares.shop.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderService {
    public static final long FREE_SHIP_FROM = 0, SHIP_FEE = 0; // giao xe miễn phí
    public static final double DEPOSIT_RATE = 0.1; // đặt cọc 10%
    public static final double INSTALLMENT_INTEREST_MONTHLY = 0.005; // lãi phẳng 0,5%/tháng
    public static final double INSTALLMENT_DOWN_RATE = 0.2; // trả trước 20%

    private final ProductRepo products; private final PromotionRepo promos; private final OrderRepo orders;

    private final NotifyService notify;
    public OrderService(ProductRepo products, PromotionRepo promos, OrderRepo orders, NotifyService notify) {
        this.notify = notify;
        this.products = products; this.promos = promos; this.orders = orders;
    }

    public record ItemReq(Long productId, int quantity, String paint, String wheel, String interior) {}
    public record CheckoutReq(String buyerName, String phone, String email, String shippingAddress, String note,
                              String promoCode, Enums.PaymentMethod paymentMethod, Integer installmentMonths, List<ItemReq> items) {}

    public Promotion validPromo(String code, long subtotal) {
        Promotion p = promos.findByCodeIgnoreCase(code == null ? "" : code.trim())
                .filter(Promotion::isValidNow).orElseThrow(() -> ApiException.bad("Mã ưu đãi không hợp lệ hoặc đã hết hạn"));
        if (subtotal < p.minOrder) throw ApiException.bad("Đơn hàng tối thiểu " + String.format("%,d", p.minOrder) + "đ để dùng mã này");
        return p;
    }

    public long discount(Promotion p, long subtotal) {
        long d = subtotal * p.discountPercent / 100;
        return p.maxDiscount > 0 ? Math.min(d, p.maxDiscount) : d;
    }

    @Transactional
    public CustomerOrder place(User user, CheckoutReq r) {
        if (r.items() == null || r.items().isEmpty()) throw ApiException.bad("Giỏ hàng trống");
        if (isBlank(r.buyerName()) || isBlank(r.phone()) || isBlank(r.shippingAddress()))
            throw ApiException.bad("Vui lòng nhập đủ họ tên, số điện thoại và địa chỉ nhận hàng");
        if (r.paymentMethod() == null) throw ApiException.bad("Vui lòng chọn phương thức thanh toán");

        CustomerOrder o = new CustomerOrder();
        o.user = user; o.buyerName = r.buyerName(); o.phone = r.phone(); o.email = r.email();
        o.shippingAddress = r.shippingAddress(); o.note = r.note(); o.paymentMethod = r.paymentMethod();
        for (ItemReq it : r.items()) {
            if (it.quantity() <= 0) throw ApiException.bad("Số lượng không hợp lệ");
            Product p = products.findById(it.productId()).filter(x -> x.active)
                    .orElseThrow(() -> ApiException.bad("Sản phẩm không tồn tại"));
            if (p.stock < it.quantity()) throw ApiException.bad("\"" + p.name + "\" chỉ còn " + p.stock + " sản phẩm");
            long unit = p.price;
            String options = null;
            if (p.customizable && p.type != Enums.ProductType.ACCESSORY) {
                var paint = CustomizerOptions.find(CustomizerOptions.PAINTS, it.paint());
                var wheel = CustomizerOptions.find(CustomizerOptions.WHEELS, it.wheel());
                var interior = CustomizerOptions.find(CustomizerOptions.INTERIORS, it.interior());
                unit += paint.price() + wheel.price() + interior.price();
                options = "Sơn: " + paint.name() + "; Mâm: " + wheel.name() + "; Nội thất: " + interior.name();
            }
            p.stock -= it.quantity(); p.sold += it.quantity();
            OrderItem oi = new OrderItem();
            oi.order = o; oi.productId = p.id; oi.productName = p.name; oi.fuel = p.fuel;
            oi.unitPrice = unit; oi.quantity = it.quantity(); oi.options = options;
            o.items.add(oi);
            o.subtotal += unit * it.quantity();
        }
        if (!isBlank(r.promoCode())) {
            Promotion p = validPromo(r.promoCode(), o.subtotal);
            o.promoCode = p.code; o.discount = discount(p, o.subtotal);
        }
        o.shippingFee = o.subtotal - o.discount >= FREE_SHIP_FROM ? 0 : SHIP_FEE;
        o.total = o.subtotal - o.discount + o.shippingFee;

        // ===== Thanh toán giả lập (mock) =====
        switch (r.paymentMethod()) {
            case FULL -> { o.paidAmount = o.total; o.paymentStatus = Enums.PaymentStatus.PAID; }
            case DEPOSIT -> { o.paidAmount = Math.round(o.total * DEPOSIT_RATE); o.paymentStatus = Enums.PaymentStatus.DEPOSITED; }
            case INSTALLMENT -> {
                int m = r.installmentMonths() == null ? 36 : r.installmentMonths();
                if (!List.of(12, 24, 36, 48, 60, 84).contains(m)) throw ApiException.bad("Kỳ hạn trả góp: 12–84 tháng");
                if (o.total < 300_000_000) throw ApiException.bad("Trả góp áp dụng cho đơn từ 300 triệu");
                long down = Math.round(o.total * INSTALLMENT_DOWN_RATE);
                long loan = o.total - down;
                o.installmentMonths = m;
                o.monthlyInstallment = (long) Math.ceil(loan * (1 + INSTALLMENT_INTEREST_MONTHLY * m) / m);
                o.paidAmount = down; o.paymentStatus = Enums.PaymentStatus.PARTIAL;
            }
            case COD -> { o.paidAmount = 0; o.paymentStatus = Enums.PaymentStatus.UNPAID; }
        }
        if (o.paidAmount > 0) o.transactionRef = "MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        o.code = "AR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss")) + (int) (Math.random() * 90 + 10);
        o.timeline.put("PENDING", LocalDateTime.now());
        CustomerOrder saved = orders.save(o);
        notify.send(user, "ORDER", "Đặt hàng thành công", "Đơn " + saved.code + " đã được ghi nhận, chúng tôi sẽ liên hệ xác nhận sớm.", "/orders/" + saved.code);
        return saved;
    }

    /** Thanh toán phần còn lại (mock). */
    @Transactional
    public CustomerOrder payRemaining(CustomerOrder o) {
        if (o.status == Enums.OrderStatus.CANCELLED) throw ApiException.bad("Đơn hàng đã huỷ");
        if (o.paymentMethod == Enums.PaymentMethod.INSTALLMENT && o.paidAmount + o.monthlyInstallment < o.total) {
            o.paidAmount += o.monthlyInstallment;
        } else if (o.paymentMethod == Enums.PaymentMethod.DEPOSIT && o.paidAmount == 0) {
            o.paidAmount = Math.round(o.total * DEPOSIT_RATE); // trả cọc (VD: đơn từ đấu giá)
        } else {
            o.paidAmount = o.total;
        }
        o.paymentStatus = o.paidAmount >= o.total ? Enums.PaymentStatus.PAID
                : (o.paymentMethod == Enums.PaymentMethod.DEPOSIT ? Enums.PaymentStatus.DEPOSITED : Enums.PaymentStatus.PARTIAL);
        o.transactionRef = "MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return orders.save(o);
    }

    @Transactional
    public CustomerOrder cancel(CustomerOrder o) {
        if (o.status == Enums.OrderStatus.CANCELLED) return o;
        if (o.status != Enums.OrderStatus.PENDING && o.status != Enums.OrderStatus.CONFIRMED)
            throw ApiException.bad("Không thể huỷ đơn đang giao hoặc đã hoàn tất");
        for (OrderItem it : o.items) if (it.productId != null) products.findById(it.productId).ifPresent(p -> { p.stock += it.quantity; p.sold -= it.quantity; });
        o.status = Enums.OrderStatus.CANCELLED;
        o.timeline.put("CANCELLED", LocalDateTime.now());
        notify.send(o.user, "ORDER", "Đơn hàng đã huỷ", "Đơn " + o.code + " đã được huỷ.", "/orders/" + o.code);
        return orders.save(o);
    }

    /** Tạo đơn cho người thắng đấu giá: đặt cọc 10% (chưa thanh toán). */
    @Transactional
    public CustomerOrder auctionOrder(User winner, String carName, long amount, String note) {
        CustomerOrder o = new CustomerOrder();
        o.user = winner; o.buyerName = winner.fullName; o.phone = winner.phone; o.email = winner.email;
        o.shippingAddress = "Nhận xe tại showroom ARES"; o.note = note;
        OrderItem oi = new OrderItem(); oi.order = o; oi.productName = carName + " (đấu giá)"; oi.unitPrice = amount; oi.quantity = 1; oi.fuel = "";
        o.items.add(oi);
        o.subtotal = amount; o.total = amount; o.paymentMethod = Enums.PaymentMethod.DEPOSIT; o.paymentStatus = Enums.PaymentStatus.UNPAID;
        o.code = "AU" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss")) + (int) (Math.random() * 90 + 10);
        o.timeline.put("PENDING", LocalDateTime.now());
        return orders.save(o);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
