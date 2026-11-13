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
    private final com.ares.shop.service.payment.PaymentProps paymentProps;

    public OrderController(OrderService service, OrderRepo orders, ReturnRepo returns, NotifyService notify,
                           com.ares.shop.service.payment.PaymentProps paymentProps) {
        this.service = service; this.orders = orders; this.returns = returns; this.notify = notify; this.paymentProps = paymentProps;
    }

    @PostMapping
    public CustomerOrder place(@RequestBody OrderService.CheckoutReq r) { return service.place(Current.user(), r); }

    @GetMapping("/{code}")
    public Map<String, Object> get(@PathVariable String code) {
        CustomerOrder o = own(code);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("order", o); m.put("returns", returns.findByOrderCode(o.code));
        m.put("bank", paymentProps.bankInfo(o.code, com.ares.shop.service.payment.PaymentConfirmationService.amountDue(o)));
        return m;
    }

    /**
     * Trước đây: khách tự bấm "đã thanh toán". Nay bị vô hiệu hoá — đơn chỉ chuyển PAID khi webhook ngân hàng
     * (SePay/payOS/Casso) hoặc IPN MoMo/VNPay có chữ ký hợp lệ báo tiền đã vào. Nhân viên vẫn có thể xác nhận thủ công
     * trong trang quản trị (AdminController.markPaid).
     */
    @PostMapping("/{code}/pay")
    public CustomerOrder pay(@PathVariable String code) {
        own(code);
        throw new ApiException(org.springframework.http.HttpStatus.GONE,
                "Đơn hàng sẽ tự động được xác nhận khi hệ thống nhận được tiền. Vui lòng hoàn tất thanh toán qua ngân hàng / MoMo / VNPay.");
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
