package com.ares.shop.web;

import com.ares.shop.model.CustomerOrder;
import com.ares.shop.model.Enums;
import com.ares.shop.model.User;
import com.ares.shop.repo.OrderRepo;
import com.ares.shop.service.MoMoService;
import com.ares.shop.service.VnPayService;
import com.ares.shop.service.payment.*;
import com.ares.shop.service.payment.PaymentConfirmationService.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Thanh toán thật:
 *  - Chuyển khoản VietQR: xác nhận tự động qua webhook SePay / payOS / Casso (bật/tắt độc lập bằng biến môi trường).
 *  - MoMo / VNPay: chỉ xác nhận qua IPN có chữ ký hợp lệ. Return URL chỉ để hiển thị kết quả.
 *  Không còn endpoint nào cho phép khách tự đánh dấu "đã thanh toán".
 */
@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final OrderRepo orders;
    private final VnPayService vnPay;
    private final MoMoService moMo;
    private final PayOsService payOs;
    private final PaymentProps props;
    private final PaymentConfirmationService confirm;
    private final Map<String, BankWebhookProvider> providers = new HashMap<>();
    private final boolean devSimulate;
    private final ObjectMapper json = new ObjectMapper();

    public PaymentController(OrderRepo orders, VnPayService vnPay, MoMoService moMo, PayOsService payOs, PaymentProps props,
                             PaymentConfirmationService confirm, List<BankWebhookProvider> list,
                             @Value("${app.payment.dev-simulate.enabled:false}") boolean devSimulate) {
        this.orders = orders; this.vnPay = vnPay; this.moMo = moMo; this.payOs = payOs; this.props = props; this.confirm = confirm;
        this.devSimulate = devSimulate;
        list.forEach(p -> providers.put(p.name().toLowerCase(Locale.ROOT), p));
    }

    // ------------------------------------------------------------------ tiện ích

    private CustomerOrder own(String code) {
        User u = Current.user();
        String c = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        return orders.findByCode(c).filter(o -> u.role != Enums.Role.USER || (o.user != null && o.user.id.equals(u.id)))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng " + code));
    }

    private static CustomerOrder payable(CustomerOrder o) {
        if (o.status == Enums.OrderStatus.CANCELLED) throw ApiException.bad("Đơn hàng đã huỷ");
        if (o.paymentStatus == Enums.PaymentStatus.PAID) throw ApiException.bad("Đơn hàng đã được thanh toán");
        if (PaymentConfirmationService.amountDue(o) <= 0) throw ApiException.bad("Đơn hàng không còn số tiền cần thanh toán");
        return o;
    }

    private static String enc(String s) { return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8); }

    private ResponseEntity<Void> redirectToPayPage(String code, String gw, String result) {
        String url = props.frontendUrl + "/pay/" + enc(code == null ? "" : code) + "?gw=" + gw + "&result=" + result;
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    private static Map<String, String> lower(HttpHeaders h) {
        Map<String, String> m = new HashMap<>();
        h.forEach((k, v) -> { if (!v.isEmpty()) m.put(k.toLowerCase(Locale.ROOT), v.get(0)); });
        return m;
    }

    private Result safeApply(java.util.function.Supplier<Result> s) {
        try { return s.get(); }
        catch (PaymentConfirmationService.DuplicateTransactionException d) { return Result.DUPLICATE; }
    }

    // ------------------------------------------------------------------ cho frontend

    /** Phương thức đang bật (để frontend ẩn/hiện). */
    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bankQr", props.bankConfigured());
        m.put("bankAutoConfirm", props.anyBankWebhook());
        m.put("sepay", providers.get("sepay").enabled());
        m.put("payos", props.payosReady());
        m.put("casso", providers.get("casso").enabled());
        m.put("momo", moMo.ready());
        m.put("vnpay", vnPay.ready());
        return m;
    }

    /** Thông tin chuyển khoản + ảnh VietQR (cùng 1 nguồn cấu hình). */
    @GetMapping("/vietqr/{code}")
    public Map<String, Object> getVietQr(@PathVariable String code) {
        CustomerOrder o = own(code);
        Map<String, Object> m = new LinkedHashMap<>(props.bankInfo(o.code, PaymentConfirmationService.amountDue(o)));
        m.put("orderCode", o.code);
        return m;
    }

    @PostMapping("/vnpay/create-url")
    public Map<String, Object> createVnPayUrl(@RequestBody Map<String, String> body, HttpServletRequest req) {
        if (!vnPay.ready()) throw new ApiException(HttpStatus.NOT_IMPLEMENTED, "VNPay chưa được cấu hình");
        CustomerOrder o = payable(own(body.get("orderCode")));
        long amount = PaymentConfirmationService.amountDue(o);
        String url = vnPay.createPaymentUrl(o, amount, req.getRemoteAddr());
        return Map.of("orderCode", o.code, "paymentUrl", url, "amount", amount);
    }

    @PostMapping("/momo/create-url")
    public Map<String, Object> createMoMoUrl(@RequestBody Map<String, String> body) {
        if (!moMo.ready()) throw new ApiException(HttpStatus.NOT_IMPLEMENTED, "MoMo chưa được cấu hình");
        CustomerOrder o = payable(own(body.get("orderCode")));
        long amount = PaymentConfirmationService.amountDue(o);
        try {
            Map<String, Object> res = moMo.createPayment(o, amount);
            res.put("orderCode", o.code); res.put("amount", amount);
            return res;
        } catch (Exception e) {
            log.warn("MoMo create failed: {}", e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Không tạo được giao dịch MoMo: " + e.getMessage());
        }
    }

    @PostMapping("/payos/create-link")
    public Map<String, Object> createPayOsLink(@RequestBody Map<String, String> body) {
        if (!props.payosReady()) throw new ApiException(HttpStatus.NOT_IMPLEMENTED, "payOS chưa được cấu hình");
        CustomerOrder o = payable(own(body.get("orderCode")));
        long amount = PaymentConfirmationService.amountDue(o);
        String back = props.frontendUrl + "/pay/" + enc(o.code) + "?gw=payos";
        try {
            Map<String, Object> res = payOs.createPaymentLink(o, amount, back, back + "&result=cancel");
            res.put("orderCode", o.code);
            return res;
        } catch (Exception e) {
            log.warn("payOS create failed: {}", e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Không tạo được link payOS: " + e.getMessage());
        }
    }

    /** Trạng thái thanh toán (frontend & màn quầy polling). Chỉ trả thông tin tối thiểu. */
    @GetMapping("/status/{code}")
    public Map<String, Object> checkStatus(@PathVariable String code) {
        CustomerOrder o = orders.findByCode(code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng " + code));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", o.code);
        m.put("paymentStatus", o.paymentStatus != null ? o.paymentStatus.name() : "UNPAID");
        m.put("status", o.status != null ? o.status.name() : "PENDING");
        m.put("total", o.total);
        m.put("paidAmount", o.paidAmount);
        m.put("amountDue", PaymentConfirmationService.amountDue(o));
        m.put("paymentMethod", o.paymentMethod != null ? o.paymentMethod.name() : "");
        m.put("paidAt", o.paidAt);
        return m;
    }

    // ------------------------------------------------------------------ VNPay

    /** IPN VNPay (server-to-server). Đăng ký URL này trong cổng merchant VNPay. */
    @GetMapping("/vnpay/ipn")
    public Map<String, String> vnPayIpn(@RequestParam Map<String, String> p) {
        try {
            if (!vnPay.verify(p)) return rsp("97", "Invalid Checksum");
            String code = p.getOrDefault("vnp_TxnRef", "");
            CustomerOrder o = confirm.find(code).orElse(null);
            if (o == null) return rsp("01", "Order not found");
            if (o.paymentStatus == Enums.PaymentStatus.PAID) return rsp("02", "Order already confirmed");
            long amount;
            try { amount = Long.parseLong(p.getOrDefault("vnp_Amount", "0")) / 100; } catch (NumberFormatException e) { return rsp("04", "Invalid amount"); }
            if (amount != PaymentConfirmationService.amountDue(o)) return rsp("04", "Invalid amount");
            boolean ok = "00".equals(p.get("vnp_ResponseCode")) && "00".equals(p.get("vnp_TransactionStatus"));
            if (!ok) return rsp("00", "Confirm Success"); // giao dịch thất bại: ghi nhận, không đổi trạng thái đơn
            String txId = p.getOrDefault("vnp_TransactionNo", "");
            if (txId.isBlank()) txId = code + ":" + p.getOrDefault("vnp_PayDate", "");
            final String fTx = txId;
            Result r = safeApply(() -> confirm.applyGateway("VNPAY", fTx, o.code, amount, json(p), Enums.PaymentMethod.VNPAY));
            return switch (r) {
                case APPLIED -> rsp("00", "Confirm Success");
                case DUPLICATE, ALREADY_PAID -> rsp("02", "Order already confirmed");
                case AMOUNT_MISMATCH, UNDERPAID -> rsp("04", "Invalid amount");
                case NO_ORDER -> rsp("01", "Order not found");
                default -> rsp("99", "Unknow error");
            };
        } catch (Exception e) {
            log.error("VNPay IPN error", e);
            return rsp("99", "Unknow error");
        }
    }

    private static Map<String, String> rsp(String code, String msg) { return Map.of("RspCode", code, "Message", msg); }

    /** Return URL VNPay: chỉ kiểm chữ ký để hiển thị, KHÔNG xác nhận tiền. */
    @GetMapping("/vnpay/return")
    public ResponseEntity<Void> vnPayReturn(@RequestParam Map<String, String> p) {
        String code = p.getOrDefault("vnp_TxnRef", "");
        if (!vnPay.verify(p)) return redirectToPayPage(code, "vnpay", "invalid");
        return redirectToPayPage(code, "vnpay", "00".equals(p.get("vnp_ResponseCode")) ? "success" : "failed");
    }

    // ------------------------------------------------------------------ MoMo

    /** IPN MoMo (POST JSON). Trả 204 khi đã nhận/xử lý. */
    @PostMapping("/momo/ipn")
    public ResponseEntity<Void> moMoIpn(@RequestBody Map<String, Object> p) {
        if (!moMo.verifyIpn(p)) {
            log.warn("MoMo IPN: sai chữ ký orderId={}", p.get("orderId"));
            return ResponseEntity.badRequest().build();
        }
        if ("0".equals(String.valueOf(p.get("resultCode")))) {
            String code = MoMoService.orderCodeOf(String.valueOf(p.get("orderId")));
            long amount = Long.parseLong(String.valueOf(p.get("amount")));
            String txId = String.valueOf(p.get("transId"));
            Result r = safeApply(() -> confirm.applyGateway("MOMO", txId, code, amount, json(p), Enums.PaymentMethod.MOMO));
            log.info("MoMo IPN order={} trans={} => {}", code, txId, r);
        }
        return ResponseEntity.noContent().build();
    }

    /** Redirect URL MoMo: chỉ hiển thị kết quả. */
    @GetMapping("/momo/return")
    public ResponseEntity<Void> moMoReturn(@RequestParam Map<String, String> p) {
        String code = MoMoService.orderCodeOf(p.get("orderId"));
        if (!moMo.verifyIpn(p)) return redirectToPayPage(code, "momo", "invalid");
        return redirectToPayPage(code, "momo", "0".equals(p.get("resultCode")) ? "success" : "failed");
    }

    // ------------------------------------------------------------------ Webhook ngân hàng (SePay / payOS / Casso)

    /** POST /api/payment/webhook/{sepay|payos|casso} */
    @PostMapping("/webhook/{provider}")
    public ResponseEntity<Map<String, Object>> bankWebhook(@PathVariable String provider, @RequestHeader HttpHeaders headers,
                                                           @RequestBody(required = false) String raw) {
        BankWebhookProvider p = providers.get(provider.toLowerCase(Locale.ROOT));
        if (p == null || !p.enabled()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "disabled"));
        if (raw == null || raw.isBlank()) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "empty body"));
        if (!p.verify(lower(headers), raw)) {
            log.warn("Webhook {}: xác thực thất bại", p.name());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "unauthorized"));
        }
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            for (BankTransfer t : p.parse(raw)) {
                String r;
                if (!t.incoming() || t.amount() <= 0) r = Result.IGNORED.name();
                else if (t.externalId() == null || t.externalId().isBlank()) r = "MISSING_ID";
                else if (!"PAYOS".equals(t.provider()) && !accountMatches(t.accountNumber())) r = "OTHER_ACCOUNT";
                else r = safeApply(() -> confirm.applyBankTransfer(t.provider(), t.externalId(), t.content(), t.amount(), raw)).name();
                results.add(Map.of("id", String.valueOf(t.externalId()), "result", r));
            }
        } catch (Exception e) {
            log.error("Webhook {} lỗi xử lý", p.name(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "error"));
        }
        // 200 + success:true để nhà cung cấp không gửi lại (kể cả khi bỏ qua giao dịch không khớp)
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true); body.put("error", 0); body.put("results", results);
        return ResponseEntity.ok(body);
    }

    private boolean accountMatches(String acc) {
        if (acc == null || acc.isBlank() || PaymentProps.isBlank(props.bankAccountNo)) return true;
        return acc.trim().equals(props.bankAccountNo.trim());
    }

    // ------------------------------------------------------------------ Chỉ dùng khi phát triển

    /**
     * Giả lập tiền vào (đi qua đúng logic đối soát). Mặc định TẮT; chỉ ADMIN, và chỉ khi PAYMENT_DEV_SIMULATE_ENABLED=true.
     */
    @PostMapping("/dev/simulate-transfer")
    public Map<String, Object> simulateTransfer(@RequestBody Map<String, Object> body) {
        if (!devSimulate) throw ApiException.notFound("Not found");
        User u = Current.user();
        if (u.role != Enums.Role.ADMIN && u.role != Enums.Role.STAFF) throw new ApiException(HttpStatus.FORBIDDEN, "Chỉ ADMIN và STAFF");
        String content = String.valueOf(body.getOrDefault("content", body.getOrDefault("orderCode", "")));
        long amount = Long.parseLong(String.valueOf(body.getOrDefault("amount", "0")));
        String id = "DEV-" + UUID.randomUUID();
        Result r = safeApply(() -> confirm.applyBankTransfer("DEV", id, content, amount, json(body)));
        return Map.of("result", r.name(), "transactionId", id);
    }

    private String json(Object o) { try { return json.writeValueAsString(o); } catch (Exception e) { return String.valueOf(o); } }
}
