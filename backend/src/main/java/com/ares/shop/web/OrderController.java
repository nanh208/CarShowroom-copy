package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.ares.shop.service.NotifyService;
import com.ares.shop.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController @RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service; private final OrderRepo orders; private final ReturnRepo returns; private final NotifyService notify;

    public OrderController(OrderService service, OrderRepo orders, ReturnRepo returns, NotifyService notify) {
        this.service = service; this.orders = orders; this.returns = returns; this.notify = notify;
    }

    @PostMapping
    public CustomerOrder place(@RequestBody OrderService.CheckoutReq r, @RequestHeader(value = "X-Aff", required = false) String aff) {
        CustomerOrder o = service.place(Current.user(), r);
        try { affiliates.attribute(o, aff); } catch (Exception ignored) {}
        return o;
    }
    @org.springframework.beans.factory.annotation.Autowired com.ares.shop.service.AffiliateService affiliates;

    @GetMapping("/{code}")
    public Map<String, Object> get(@PathVariable String code) {
        CustomerOrder o = own(code);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("order", o); m.put("returns", returns.findByOrderCode(o.code));
        m.put("bank", Map.of("bank", "Vietcombank", "bin", "970436", "account", "1023456789", "holder", "CONG TY ARES TOYS",
                "qr", "https://img.vietqr.io/image/970436-1023456789-compact2.png?amount=" + Math.max(0, o.total - o.paidAmount) + "&addInfo=" + o.code + "&accountName=CONG%20TY%20ARES%20TOYS"));
        return m;
    }

    /** Xác nhận thanh toán giả lập (QR chuyển khoản / MoMo / VNPay / kỳ trả góp). */
    @PostMapping("/{code}/pay")
    public CustomerOrder pay(@PathVariable String code) {
        CustomerOrder o = own(code);
        if (o.paymentMethod == Enums.PaymentMethod.MOMO || o.paymentMethod == Enums.PaymentMethod.VNPAY)
            throw ApiException.bad("Đơn " + o.paymentMethod + " được xác nhận qua cổng thanh toán — dùng /api/payments/" + code + "/create");
        return service.pay(o);
    }

    @PostMapping("/{code}/cancel")
    public CustomerOrder cancel(@PathVariable String code, @RequestBody(required = false) Map<String, String> body) {
        return service.cancel(own(code), body == null ? null : body.get("reason"));
    }

    public record ReturnReq(String kind, String reason, List<String> images) {}

    /** Yêu cầu đổi trả / hoàn tiền: trong 7 ngày sau khi nhận hàng. */
    @PostMapping("/{code}/returns")
    public ReturnRequest requestReturn(@PathVariable String code, @RequestBody ReturnReq r) {
        CustomerOrder o = own(code);
        if (o.status != Enums.OrderStatus.COMPLETED) throw ApiException.bad("Chỉ gửi yêu cầu đổi trả cho đơn đã nhận hàng");
        var done = o.timeline.get("COMPLETED");
        if (done != null && done.plusDays(7).isBefore(java.time.LocalDateTime.now())) throw ApiException.bad("Đã quá 7 ngày kể từ khi nhận hàng");
        if (returns.findByOrderCode(o.code).stream().anyMatch(x -> x.status == Enums.ReturnStatus.PENDING || x.status == Enums.ReturnStatus.APPROVED))
            throw ApiException.bad("Đơn hàng đã có yêu cầu đang xử lý");
        if (r.reason() == null || r.reason().trim().length() < 10) throw ApiException.bad("Vui lòng mô tả lý do (tối thiểu 10 ký tự)");
        ReturnRequest x = new ReturnRequest(); x.order = o; x.orderCode = o.code; x.user = o.user; x.customerName = o.buyerName;
        x.kind = "REFUND".equals(r.kind()) ? "REFUND" : "RETURN"; x.reason = r.reason().trim(); x.refundAmount = o.paidAmount;
        if (r.images() != null) x.images = new ArrayList<>(r.images());
        notify.send(o.user, "ORDER", "Đã nhận yêu cầu đổi trả", "Yêu cầu cho đơn " + o.code + " đang được xem xét trong 24h.", "/orders/" + o.code);
        return returns.save(x);
    }

    private CustomerOrder own(String code) {
        User u = Current.user();
        return orders.findByCode(code).filter(o -> u.role != Enums.Role.USER || (o.user != null && o.user.id.equals(u.id)))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng"));
    }
}
