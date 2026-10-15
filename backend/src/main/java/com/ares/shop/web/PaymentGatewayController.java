package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.ares.shop.service.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Cổng thanh toán kiểu VNPay / MoMo (sandbox, không gọi mạng) — mô phỏng đúng luồng thật:
 * 1) Tạo giao dịch (txnRef, số tiền, hạn 15 phút) + tham số ký HMAC-SHA512 (giống vnp_SecureHash).
 * 2) Trang cổng sandbox → khách chọn ngân hàng / xác nhận / huỷ → "cổng" gọi IPN server-to-server (ký) rồi trả về returnUrl (ký).
 * 3) IPN: kiểm chữ ký, số tiền, idempotent → đánh dấu đơn đã thanh toán. Trang return chỉ hiển thị (không ghi nhận tiền).
 * Mã phản hồi theo VNPay: 00 thành công, 24 khách huỷ, 51 không đủ số dư, 11 hết hạn, 97 sai chữ ký.
 */
@RestController
public class PaymentGatewayController {
    static final String SECRET = "ARES-SANDBOX-HASHSECRET-2026"; static final String TMN = "ARESTOYS";
    static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final PaymentTxnRepo txns; private final OrderRepo orders; private final OrderService orderService;
    public PaymentGatewayController(PaymentTxnRepo txns, OrderRepo orders, OrderService orderService) { this.txns = txns; this.orders = orders; this.orderService = orderService; }

    static String hmac(String data) {
        try { Mac m = Mac.getInstance("HmacSHA512"); m.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            StringBuilder sb = new StringBuilder(); for (byte b : m.doFinal(data.getBytes(StandardCharsets.UTF_8))) sb.append(String.format("%02x", b)); return sb.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    /** Chuỗi ký: các tham số vnp_* (trừ hash) sắp xếp theo tên, key=urlencode(value) nối bằng & — như VNPay. */
    static String canonical(Map<String, String> p) {
        StringBuilder sb = new StringBuilder();
        new TreeMap<>(p).forEach((k, v) -> { if (k.equals("vnp_SecureHash") || v == null || v.isEmpty()) return; if (sb.length() > 0) sb.append('&'); sb.append(k).append('=').append(URLEncoder.encode(v, StandardCharsets.US_ASCII)); });
        return sb.toString();
    }
    static Map<String, String> sign(Map<String, String> p) { p.put("vnp_SecureHash", hmac(canonical(p))); return p; }
    static boolean verify(Map<String, String> p) { String h = p.get("vnp_SecureHash"); return h != null && h.equalsIgnoreCase(hmac(canonical(p))); }

    private CustomerOrder own(String code) {
        CustomerOrder o = orders.findByCode(code).orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn"));
        User u = Current.user(); if (o.user == null || !o.user.id.equals(u.id)) throw ApiException.notFound("Không tìm thấy đơn");
        return o;
    }

    public record CreateReq(String provider) {}
    /** Bước 1: tạo giao dịch + tham số đã ký để chuyển sang cổng. */
    @PostMapping("/api/payments/{code}/create") @Transactional
    public Map<String, Object> create(@PathVariable String code, @RequestBody(required = false) CreateReq r) {
        CustomerOrder o = own(code);
        if (o.status == Enums.OrderStatus.CANCELLED) throw ApiException.bad("Đơn đã huỷ");
        if (o.paymentStatus == Enums.PaymentStatus.PAID) throw ApiException.bad("Đơn đã được thanh toán");
        String provider = r == null || r.provider() == null ? String.valueOf(o.paymentMethod) : r.provider().toUpperCase();
        if (!List.of("VNPAY", "MOMO").contains(provider)) throw ApiException.bad("Cổng thanh toán không hỗ trợ");
        // huỷ giao dịch đang chờ cũ của đơn
        for (PaymentTxn t : txns.findByOrderCodeOrderByIdDesc(code)) if ("PENDING".equals(t.status)) { t.status = "CANCELLED"; t.responseCode = "24"; t.completedAt = LocalDateTime.now(); txns.save(t); }
        PaymentTxn t = new PaymentTxn(); t.orderCode = code; t.provider = provider; t.amount = Math.max(0, o.total - o.paidAmount);
        if (t.amount <= 0) throw ApiException.bad("Không còn số tiền cần thanh toán");
        t.txnRef = code + "-" + Long.toString(System.nanoTime() % 1_000_000_000L, 36).toUpperCase(); t.expiresAt = LocalDateTime.now().plusMinutes(15);
        txns.save(t);
        Map<String, String> p = new LinkedHashMap<>();
        p.put("vnp_Version", "2.1.0"); p.put("vnp_Command", "pay"); p.put("vnp_TmnCode", TMN); p.put("vnp_Amount", String.valueOf(t.amount * 100));
        p.put("vnp_CurrCode", "VND"); p.put("vnp_TxnRef", t.txnRef); p.put("vnp_OrderInfo", "Thanh toan don hang " + code); p.put("vnp_Locale", "vn");
        p.put("vnp_CreateDate", t.createdAt.format(F)); p.put("vnp_ExpireDate", t.expiresAt.format(F)); p.put("vnp_ReturnUrl", "/pay/return"); p.put("x_Provider", provider);
        sign(p);
        return Map.of("txnRef", t.txnRef, "provider", provider, "amount", t.amount, "expiresAt", t.expiresAt, "params", p, "orderCode", code);
    }

    /** Trang cổng sandbox lấy thông tin giao dịch (kiểm chữ ký tham số tạo). */
    @PostMapping("/api/public/payments/sandbox/inspect")
    public Map<String, Object> inspect(@RequestBody Map<String, String> p) {
        if (!verify(p)) throw ApiException.bad("Sai chữ ký yêu cầu thanh toán (97)");
        PaymentTxn t = txns.findByTxnRef(p.get("vnp_TxnRef")).orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch"));
        if (Long.parseLong(p.get("vnp_Amount")) != t.amount * 100) throw ApiException.bad("Số tiền không khớp");
        return Map.of("txnRef", t.txnRef, "provider", t.provider, "amount", t.amount, "status", t.status, "expiresAt", t.expiresAt, "orderCode", t.orderCode,
            "banks", "VNPAY".equals(t.provider) ? List.of("NCB", "VCB", "TCB", "BIDV", "INTCARD") : List.of("MOMO"));
    }

    public record CompleteReq(String result, String bankCode) {}
    /** Bước 2 (phía "cổng"): khách xác nhận/huỷ → cổng gọi IPN tới shop rồi trả về tham số return đã ký. */
    @PostMapping("/api/public/payments/sandbox/{txnRef}/complete")
    public Map<String, Object> complete(@PathVariable String txnRef, @RequestBody CompleteReq r) {
        PaymentTxn t = txns.findByTxnRef(txnRef).orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch"));
        String rc = switch (String.valueOf(r.result())) { case "success" -> "00"; case "cancel" -> "24"; case "insufficient" -> "51"; default -> throw ApiException.bad("Kết quả không hợp lệ"); };
        if (t.expiresAt != null && LocalDateTime.now().isAfter(t.expiresAt)) rc = "11";
        Map<String, String> p = new LinkedHashMap<>();
        p.put("vnp_TmnCode", TMN); p.put("vnp_Amount", String.valueOf(t.amount * 100)); p.put("vnp_TxnRef", t.txnRef); p.put("vnp_ResponseCode", rc);
        p.put("vnp_TransactionStatus", rc); p.put("vnp_BankCode", r.bankCode() == null ? ("MOMO".equals(t.provider) ? "MOMO" : "NCB") : r.bankCode());
        p.put("vnp_TransactionNo", String.valueOf(10_000_000 + new Random().nextInt(89_999_999))); p.put("vnp_PayDate", LocalDateTime.now().format(F));
        sign(p);
        Map<String, Object> ipn = ipn(new LinkedHashMap<>(p)); // server-to-server
        return Map.of("returnParams", p, "ipn", ipn);
    }

    /** IPN: nguồn sự thật để ghi nhận tiền. Trả RspCode theo chuẩn VNPay. Idempotent. */
    @PostMapping("/api/public/payments/ipn") @Transactional
    public Map<String, Object> ipn(@RequestBody Map<String, String> p) {
        if (!verify(p)) return Map.of("RspCode", "97", "Message", "Invalid signature");
        PaymentTxn t = txns.findByTxnRef(p.get("vnp_TxnRef")).orElse(null);
        if (t == null) return Map.of("RspCode", "01", "Message", "Order not found");
        t.ipnCount++;
        if (Long.parseLong(p.getOrDefault("vnp_Amount", "0")) != t.amount * 100) { txns.save(t); return Map.of("RspCode", "04", "Message", "Invalid amount"); }
        if (!"PENDING".equals(t.status)) { txns.save(t); return Map.of("RspCode", "02", "Message", "Order already confirmed"); }
        String rc = p.get("vnp_ResponseCode");
        t.responseCode = rc; t.bankCode = p.get("vnp_BankCode"); t.gatewayTxnNo = p.get("vnp_TransactionNo"); t.completedAt = LocalDateTime.now();
        t.status = switch (rc) { case "00" -> "SUCCESS"; case "24" -> "CANCELLED"; case "11" -> "EXPIRED"; default -> "FAILED"; };
        txns.save(t);
        if ("SUCCESS".equals(t.status)) {
            CustomerOrder o = orders.findByCode(t.orderCode).orElseThrow();
            if (o.paymentStatus != Enums.PaymentStatus.PAID) { orderService.pay(o); CustomerOrder f = orders.findByCode(t.orderCode).orElseThrow(); f.transactionRef = t.provider + "-" + t.gatewayTxnNo; orders.save(f); }
        }
        return Map.of("RspCode", "00", "Message", "Confirm Success");
    }

    static final Map<String, String> MSG = Map.of("00", "Giao dịch thành công", "24", "Bạn đã huỷ giao dịch", "51", "Tài khoản không đủ số dư", "11", "Giao dịch đã hết hạn", "97", "Chữ ký không hợp lệ");
    /** Trang return: kiểm chữ ký, đọc trạng thái đã ghi nhận qua IPN (không tự ghi nhận tiền). */
    @GetMapping("/api/public/payments/return")
    public Map<String, Object> ret(@RequestParam Map<String, String> p) {
        boolean ok = verify(p);
        PaymentTxn t = txns.findByTxnRef(String.valueOf(p.get("vnp_TxnRef"))).orElse(null);
        String rc = ok ? p.get("vnp_ResponseCode") : "97";
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("valid", ok); m.put("responseCode", rc); m.put("message", MSG.getOrDefault(rc, "Giao dịch không thành công (mã " + rc + ")"));
        m.put("status", t == null ? "UNKNOWN" : t.status); m.put("orderCode", t == null ? null : t.orderCode); m.put("amount", t == null ? 0 : t.amount); m.put("provider", t == null ? null : t.provider);
        m.put("success", ok && t != null && "SUCCESS".equals(t.status));
        return m;
    }

    @GetMapping("/api/payments/{code}/transactions")
    public List<PaymentTxn> mine(@PathVariable String code) { own(code); return txns.findByOrderCodeOrderByIdDesc(code); }

    @GetMapping("/api/admin/payments")
    public List<PaymentTxn> all() { return txns.findAllByOrderByIdDesc(); }

    /** Hết hạn giao dịch chờ quá 15 phút. */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void expire() {
        for (PaymentTxn t : txns.findByStatus("PENDING")) if (t.expiresAt != null && LocalDateTime.now().isAfter(t.expiresAt)) { t.status = "EXPIRED"; t.responseCode = "11"; t.completedAt = LocalDateTime.now(); txns.save(t); }
    }
}
