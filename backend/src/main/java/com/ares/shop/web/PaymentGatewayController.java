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
import java.time.ZonedDateTime;
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
    private final boolean sandbox; private final com.ares.shop.service.GatewayClient gw; private final int expireMin;
    public PaymentGatewayController(PaymentTxnRepo txns, OrderRepo orders, OrderService orderService, com.ares.shop.service.GatewayClient gw,
                                    @org.springframework.beans.factory.annotation.Value("${app.payments.sandbox.enabled:true}") boolean sandbox,
                                    @org.springframework.beans.factory.annotation.Value("${app.payments.expire-minutes:15}") int expireMin) {
        this.txns = txns; this.orders = orders; this.orderService = orderService; this.sandbox = sandbox; this.gw = gw; this.expireMin = expireMin;
    }
    static String clientIp(jakarta.servlet.http.HttpServletRequest rq) {
        String x = rq.getHeader("X-Forwarded-For"); String ip = x != null && !x.isBlank() ? x.split(",")[0].trim() : rq.getRemoteAddr();
        return ip == null || ip.isBlank() || ip.contains(":") ? "127.0.0.1" : ip; // VNPay nhận IPv4
    }
    /** Cổng sandbox chỉ chạy khi bật cờ (mặc định bật ở dev; profile prod tắt). Tắt → 404 như không tồn tại. */
    private void requireSandbox() { if (!sandbox) throw ApiException.notFound("Cổng sandbox đã tắt"); }
    /** Token hoàn tất: chỉ người nhận được phản hồi create (chủ đơn) mới hoàn tất được giao dịch sandbox. */
    static String sandboxToken(String txnRef) { return hmac("sandbox-complete|" + txnRef).substring(0, 32); }
    @GetMapping("/api/public/payments/sandbox/status")
    public Map<String, Object> sandboxStatus() { return Map.of("enabled", sandbox); }

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

    public record CreateReq(String provider, String method, String bankCode, String lang) {}
    /** Bước 1: tạo giao dịch. Cổng thật (có khoá) → trả redirectUrl sang VNPay/MoMo; không có khoá → cổng giả lập nội bộ (nếu bật sandbox). */
    @PostMapping("/api/payments/{code}/create") @Transactional
    public Map<String, Object> create(@PathVariable String code, @RequestBody(required = false) CreateReq r, jakarta.servlet.http.HttpServletRequest rq) {
        CustomerOrder o = own(code);
        if (o.status == Enums.OrderStatus.CANCELLED) throw ApiException.bad("Đơn đã huỷ");
        if (o.paymentStatus == Enums.PaymentStatus.PAID) throw ApiException.bad("Đơn đã được thanh toán");
        String provider = r == null || r.provider() == null ? String.valueOf(o.paymentMethod) : r.provider().toUpperCase();
        if (!List.of("VNPAY", "MOMO").contains(provider)) throw ApiException.bad("Cổng thanh toán không hỗ trợ");
        boolean real = "VNPAY".equals(provider) ? gw.vnpayReady() : gw.momoReady();
        if (!real && !sandbox) throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Cổng " + provider + " chưa được cấu hình");
        // Idempotent theo đơn: huỷ giao dịch đang chờ cũ (cổng thật sẽ tự hết hạn theo vnp_ExpireDate / phiên MoMo)
        for (PaymentTxn t : txns.findByOrderCodeOrderByIdDesc(code)) if ("PENDING".equals(t.status)) { t.status = "CANCELLED"; t.responseCode = "24"; t.completedAt = LocalDateTime.now(); txns.save(t); }
        PaymentTxn t = new PaymentTxn(); t.orderCode = code; t.provider = provider; t.amount = Math.max(0, o.total - o.paidAmount);
        if (t.amount <= 0) throw ApiException.bad("Không còn số tiền cần thanh toán");
        if (real && "MOMO".equals(provider) && (t.amount < 1000 || t.amount > 50_000_000)) throw ApiException.bad("MoMo chỉ nhận 1.000đ – 50.000.000đ mỗi giao dịch");
        t.txnRef = code + "-" + Long.toString(System.nanoTime() % 1_000_000_000L, 36).toUpperCase(); t.expiresAt = LocalDateTime.now().plusMinutes(expireMin);
        t.method = r == null ? null : r.method(); String lang = r == null ? null : r.lang();
        if (real) {
            t.mode = "REAL"; ZonedDateTime now = ZonedDateTime.now(com.ares.shop.service.GatewayClient.VN); t.gwCreateDate = now.format(F);
            if ("VNPAY".equals(provider)) {
                txns.save(t);
                String url = gw.vnPayUrl(t.txnRef, t.amount, "Thanh toan don hang " + code, clientIp(rq), t.gwCreateDate, now.plusMinutes(expireMin).format(F), lang, r == null ? null : r.bankCode());
                return Map.of("txnRef", t.txnRef, "provider", provider, "amount", t.amount, "expiresAt", t.expiresAt, "orderCode", code, "real", true, "redirectUrl", url, "sandbox", false);
            }
            String requestType = "atm".equalsIgnoreCase(t.method) || "payWithATM".equals(t.method) ? "payWithATM" : "captureWallet";
            txns.save(t);
            try {
                Map<String, Object> m = gw.momoCreate(t.txnRef, t.txnRef, t.amount, "Thanh toan don hang " + code, requestType, lang);
                if ("0".equals(String.valueOf(m.get("resultCode"))) && m.get("payUrl") != null && !Boolean.FALSE.equals(m.get("_signatureValid")))
                    return Map.of("txnRef", t.txnRef, "provider", provider, "amount", t.amount, "expiresAt", t.expiresAt, "orderCode", code, "real", true, "redirectUrl", m.get("payUrl"), "sandbox", false);
                t.note = "MoMo create: " + m.get("resultCode") + " " + m.get("message");
            } catch (IllegalStateException e) { t.note = e.getMessage(); }
            t.status = "FAILED"; t.completedAt = LocalDateTime.now(); txns.save(t);
            if (!sandbox) throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY, "MoMo từ chối/không phản hồi: " + t.note);
            // dev: rơi về cổng giả lập để không chặn luồng thử nghiệm
            PaymentTxn f = new PaymentTxn(); f.orderCode = code; f.provider = provider; f.amount = t.amount; f.expiresAt = t.expiresAt; f.method = t.method;
            f.txnRef = code + "-" + Long.toString((System.nanoTime() + 7) % 1_000_000_000L, 36).toUpperCase(); f.note = "Fallback mock: " + t.note; t = f;
        }
        txns.save(t);
        Map<String, String> p = new LinkedHashMap<>();
        p.put("vnp_Version", "2.1.0"); p.put("vnp_Command", "pay"); p.put("vnp_TmnCode", TMN); p.put("vnp_Amount", String.valueOf(t.amount * 100));
        p.put("vnp_CurrCode", "VND"); p.put("vnp_TxnRef", t.txnRef); p.put("vnp_OrderInfo", "Thanh toan don hang " + code); p.put("vnp_Locale", "vn");
        p.put("vnp_CreateDate", t.createdAt.format(F)); p.put("vnp_ExpireDate", t.expiresAt.format(F)); p.put("vnp_ReturnUrl", "/pay/return"); p.put("x_Provider", provider);
        sign(p);
        Map<String, Object> out = new LinkedHashMap<>(Map.of("txnRef", t.txnRef, "provider", provider, "amount", t.amount, "expiresAt", t.expiresAt, "params", p, "orderCode", code, "sandbox", sandbox, "real", false));
        if (t.note != null) out.put("fallbackReason", t.note);
        out.put("sandboxToken", sandboxToken(t.txnRef));
        return out;
    }

    /** Trang cổng sandbox lấy thông tin giao dịch (kiểm chữ ký tham số tạo). */
    @PostMapping("/api/public/payments/sandbox/inspect")
    public Map<String, Object> inspect(@RequestBody Map<String, String> p) {
        requireSandbox();
        if (!verify(p)) throw ApiException.bad("Sai chữ ký yêu cầu thanh toán (97)");
        PaymentTxn t = txns.findByTxnRef(p.get("vnp_TxnRef")).orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch"));
        if ("REAL".equals(t.mode)) throw ApiException.notFound("Không tìm thấy giao dịch");
        if (Long.parseLong(p.get("vnp_Amount")) != t.amount * 100) throw ApiException.bad("Số tiền không khớp");
        return Map.of("txnRef", t.txnRef, "provider", t.provider, "amount", t.amount, "status", t.status, "expiresAt", t.expiresAt, "orderCode", t.orderCode,
            "banks", "VNPAY".equals(t.provider) ? List.of("NCB", "VCB", "TCB", "BIDV", "INTCARD") : List.of("MOMO"));
    }

    public record CompleteReq(String result, String bankCode, String token) {}
    /** Bước 2 (phía "cổng"): khách xác nhận/huỷ → cổng gọi IPN tới shop rồi trả về tham số return đã ký. */
    @PostMapping("/api/public/payments/sandbox/{txnRef}/complete")
    public Map<String, Object> complete(@PathVariable String txnRef, @RequestBody CompleteReq r) {
        requireSandbox();
        if (r.token() == null || !java.security.MessageDigest.isEqual(r.token().getBytes(StandardCharsets.UTF_8), sandboxToken(txnRef).getBytes(StandardCharsets.UTF_8)))
            throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "Thiếu hoặc sai token giao dịch sandbox");
        PaymentTxn t = txns.findByTxnRef(txnRef).orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch"));
        if ("REAL".equals(t.mode)) throw ApiException.notFound("Không tìm thấy giao dịch"); // giao dịch thật chỉ ghi nhận qua IPN có chữ ký cổng
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
        if (t == null || "REAL".equals(t.mode)) return Map.of("RspCode", "01", "Message", "Order not found");
        t.ipnCount++;
        if (Long.parseLong(p.getOrDefault("vnp_Amount", "0")) != t.amount * 100) { txns.save(t); return Map.of("RspCode", "04", "Message", "Invalid amount"); }
        if (!"PENDING".equals(t.status)) { txns.save(t); return Map.of("RspCode", "02", "Message", "Order already confirmed"); }
        String rc = p.get("vnp_ResponseCode");
        settle(t, rc, switch (rc) { case "00" -> "SUCCESS"; case "24" -> "CANCELLED"; case "11" -> "EXPIRED"; default -> "FAILED"; }, p.get("vnp_TransactionNo"), p.get("vnp_BankCode"));
        return Map.of("RspCode", "00", "Message", "Confirm Success");
    }

    /** Ghi nhận kết quả (dùng chung mock/VNPay/MoMo). Đơn chỉ được đánh dấu PAID một lần. */
    void settle(PaymentTxn t, String rc, String status, String txnNo, String bank) {
        t.responseCode = rc; t.bankCode = bank; t.gatewayTxnNo = txnNo; t.completedAt = LocalDateTime.now(); t.status = status; txns.save(t);
        if ("SUCCESS".equals(status)) {
            CustomerOrder o = orders.findByCode(t.orderCode).orElseThrow();
            if (o.paymentStatus != Enums.PaymentStatus.PAID) { orderService.pay(o); CustomerOrder f = orders.findByCode(t.orderCode).orElseThrow(); f.transactionRef = t.provider + "-" + txnNo; orders.save(f); }
        }
    }
    /** Giao dịch còn nhận kết quả: PENDING, hoặc EXPIRED cục bộ nhưng cổng báo thành công (khách đã bị trừ tiền → vẫn ghi nhận). */
    static boolean open(PaymentTxn t, boolean success) { return "PENDING".equals(t.status) || (success && "EXPIRED".equals(t.status)); }

    // ===================== VNPay thật =====================
    /** IPN VNPay (GET, server-to-server). Thứ tự kiểm theo tài liệu: checksum(97) → đơn(01) → số tiền(04) → trạng thái(02) → cập nhật(00). */
    @GetMapping("/api/public/payments/vnpay/ipn") @Transactional
    public Map<String, Object> vnpayIpn(@RequestParam Map<String, String> p) {
        try {
            if (p.isEmpty()) return Map.of("RspCode", "99", "Message", "Input data required");
            if (!gw.vnVerify(p)) return Map.of("RspCode", "97", "Message", "Invalid signature");
            PaymentTxn t = txns.findByTxnRef(p.get("vnp_TxnRef")).filter(x -> "REAL".equals(x.mode) && "VNPAY".equals(x.provider)).orElse(null);
            if (t == null || !gw.vnTmn.equals(p.get("vnp_TmnCode"))) return Map.of("RspCode", "01", "Message", "Order not found");
            t.ipnCount++; txns.save(t);
            if (!String.valueOf(t.amount * 100).equals(p.get("vnp_Amount"))) return Map.of("RspCode", "04", "Message", "Invalid amount");
            boolean ok = "00".equals(p.get("vnp_ResponseCode")) && "00".equals(p.get("vnp_TransactionStatus"));
            if (!open(t, ok)) return Map.of("RspCode", "02", "Message", "Order already confirmed");
            String rc = p.get("vnp_ResponseCode");
            settle(t, rc, ok ? "SUCCESS" : "24".equals(rc) ? "CANCELLED" : "11".equals(rc) ? "EXPIRED" : "FAILED", p.get("vnp_TransactionNo"), p.get("vnp_BankCode"));
            return Map.of("RspCode", "00", "Message", "Confirm Success");
        } catch (Exception e) { return Map.of("RspCode", "99", "Message", "Unknow error"); }
    }
    static final Map<String, String> VN_MSG = Map.ofEntries(Map.entry("00", "Giao dịch thành công"), Map.entry("07", "Trừ tiền thành công, giao dịch bị nghi ngờ"),
        Map.entry("09", "Thẻ/Tài khoản chưa đăng ký InternetBanking"), Map.entry("10", "Xác thực thông tin thẻ/tài khoản sai quá 3 lần"), Map.entry("11", "Giao dịch đã hết hạn"),
        Map.entry("12", "Thẻ/Tài khoản bị khoá"), Map.entry("13", "Sai mật khẩu OTP"), Map.entry("24", "Bạn đã huỷ giao dịch"), Map.entry("51", "Tài khoản không đủ số dư"),
        Map.entry("65", "Vượt hạn mức giao dịch trong ngày"), Map.entry("75", "Ngân hàng đang bảo trì"), Map.entry("79", "Sai mật khẩu thanh toán quá số lần quy định"), Map.entry("97", "Chữ ký không hợp lệ"));
    /** Return URL VNPay: chỉ kiểm chữ ký + hiển thị. Nếu IPN chưa tới (vd. chạy localhost) → gọi querydr để đối soát chính thức rồi mới ghi nhận. */
    @GetMapping("/api/public/payments/vnpay/return") @Transactional
    public Map<String, Object> vnpayReturn(@RequestParam Map<String, String> p, jakarta.servlet.http.HttpServletRequest rq) {
        boolean ok = gw.vnVerify(p); PaymentTxn t = txns.findByTxnRef(String.valueOf(p.get("vnp_TxnRef"))).filter(x -> "REAL".equals(x.mode)).orElse(null);
        if (ok && t != null && "PENDING".equals(t.status)) reconcile(t, clientIp(rq));
        String rc = ok ? p.get("vnp_ResponseCode") : "97";
        return result(t, ok, rc, VN_MSG.getOrDefault(rc, "Giao dịch không thành công (mã " + rc + ")"));
    }
    Map<String, Object> result(PaymentTxn t, boolean valid, String rc, String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("valid", valid); m.put("responseCode", rc); m.put("message", msg);
        m.put("status", t == null ? "UNKNOWN" : t.status); m.put("orderCode", t == null ? null : t.orderCode); m.put("amount", t == null ? 0 : t.amount); m.put("provider", t == null ? null : t.provider);
        m.put("success", valid && t != null && "SUCCESS".equals(t.status)); m.put("real", true);
        return m;
    }

    // ===================== MoMo thật =====================
    /** IPN MoMo (POST JSON). Kiểm chữ ký HMAC-SHA256, partnerCode, số tiền; idempotent. Trả 204 như tài liệu yêu cầu. */
    @PostMapping("/api/public/payments/momo/ipn") @Transactional
    public org.springframework.http.ResponseEntity<Void> momoIpn(@RequestBody Map<String, Object> p) {
        if (!gw.momoVerify(p) || !gw.momoPartner.equals(String.valueOf(p.get("partnerCode")))) return org.springframework.http.ResponseEntity.badRequest().build();
        PaymentTxn t = txns.findByTxnRef(String.valueOf(p.get("orderId"))).filter(x -> "REAL".equals(x.mode) && "MOMO".equals(x.provider)).orElse(null);
        if (t == null) return org.springframework.http.ResponseEntity.notFound().build();
        t.ipnCount++; txns.save(t);
        if (!String.valueOf(t.amount).equals(String.valueOf(p.get("amount")))) return org.springframework.http.ResponseEntity.badRequest().build();
        String rc = String.valueOf(p.get("resultCode")); boolean ok = "0".equals(rc) || "9000".equals(rc);
        if (open(t, ok) && !"7000".equals(rc) && !"7002".equals(rc) && !"1000".equals(rc)) settle(t, rc, ok ? "SUCCESS" : "1006".equals(rc) ? "CANCELLED" : "1005".equals(rc) ? "EXPIRED" : "FAILED", String.valueOf(p.get("transId")), String.valueOf(p.getOrDefault("payType", "momo")));
        return org.springframework.http.ResponseEntity.noContent().build();
    }
    static final Map<String, String> MOMO_MSG = Map.of("0", "Giao dịch thành công", "9000", "Giao dịch đã được xác nhận", "1005", "Giao dịch đã hết hạn", "1006", "Bạn đã huỷ giao dịch",
        "1001", "Tài khoản không đủ số dư", "1003", "Giao dịch đã bị huỷ", "1000", "Giao dịch đang chờ người dùng xác nhận", "7000", "Giao dịch đang được xử lý");
    @GetMapping("/api/public/payments/momo/return") @Transactional
    public Map<String, Object> momoReturn(@RequestParam Map<String, String> p, jakarta.servlet.http.HttpServletRequest rq) {
        boolean ok = gw.momoVerify(p); PaymentTxn t = txns.findByTxnRef(String.valueOf(p.get("orderId"))).filter(x -> "REAL".equals(x.mode)).orElse(null);
        if (ok && t != null && "PENDING".equals(t.status)) reconcile(t, clientIp(rq));
        String rc = ok ? String.valueOf(p.get("resultCode")) : "97";
        return result(t, ok, rc, ok ? MOMO_MSG.getOrDefault(rc, "Giao dịch không thành công (mã " + rc + ")") : "Chữ ký không hợp lệ");
    }

    /** Đối soát chủ động với cổng (querydr / MoMo query). Chỉ ghi nhận khi phản hồi hợp lệ & khớp số tiền. */
    Map<String, Object> reconcile(PaymentTxn t, String ip) {
        Map<String, Object> r;
        try {
            if ("VNPAY".equals(t.provider)) {
                r = gw.vnQuery(t.txnRef, t.gwCreateDate, ip);
                if (Boolean.TRUE.equals(r.get("_signatureValid")) && "00".equals(r.get("vnp_ResponseCode")) && String.valueOf(t.amount * 100).equals(String.valueOf(r.get("vnp_Amount")))) {
                    String ts = String.valueOf(r.get("vnp_TransactionStatus"));
                    if ("00".equals(ts) && open(t, true)) settle(t, "00", "SUCCESS", String.valueOf(r.get("vnp_TransactionNo")), String.valueOf(r.get("vnp_BankCode")));
                    else if ("02".equals(ts) && "PENDING".equals(t.status)) settle(t, ts, "FAILED", String.valueOf(r.get("vnp_TransactionNo")), String.valueOf(r.get("vnp_BankCode")));
                }
            } else {
                r = gw.momoQuery(t.txnRef);
                String rc = String.valueOf(r.get("resultCode"));
                if (gw.momoPartner.equals(String.valueOf(r.get("partnerCode"))) && String.valueOf(t.amount).equals(String.valueOf(r.get("amount")))) {
                    if (("0".equals(rc) || "9000".equals(rc)) && open(t, true)) settle(t, rc, "SUCCESS", String.valueOf(r.get("transId")), String.valueOf(r.getOrDefault("payType", "momo")));
                    else if (List.of("1005", "1006", "1003", "1001").contains(rc) && "PENDING".equals(t.status)) settle(t, rc, "1006".equals(rc) ? "CANCELLED" : "1005".equals(rc) ? "EXPIRED" : "FAILED", String.valueOf(r.get("transId")), null);
                }
            }
        } catch (IllegalStateException e) { r = Map.of("error", e.getMessage()); }
        r = new LinkedHashMap<>(r); r.remove("signature"); r.remove("vnp_SecureHash"); r.put("localStatus", t.status);
        return r;
    }
    @PostMapping("/api/admin/payments/{txnRef}/query") @Transactional
    public Map<String, Object> adminQuery(@PathVariable String txnRef, jakarta.servlet.http.HttpServletRequest rq) {
        PaymentTxn t = txns.findByTxnRef(txnRef).orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch"));
        if (!"REAL".equals(t.mode)) return Map.of("localStatus", t.status, "mode", "MOCK", "message", "Giao dịch giả lập — không có cổng thật để truy vấn");
        return reconcile(t, clientIp(rq));
    }
    public record RefundReq(Long amount) {}
    /** Hoàn tiền qua cổng thật (VNPay refund 02/03, MoMo /refund). Không vượt quá số đã thu − đã hoàn. */
    @PostMapping("/api/admin/payments/{txnRef}/refund") @Transactional
    public Map<String, Object> adminRefund(@PathVariable String txnRef, @RequestBody(required = false) RefundReq r, jakarta.servlet.http.HttpServletRequest rq) {
        PaymentTxn t = txns.findByTxnRef(txnRef).orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch"));
        if (!"REAL".equals(t.mode)) throw ApiException.bad("Chỉ hoàn tiền qua cổng cho giao dịch thật");
        if (!"SUCCESS".equals(t.status)) throw ApiException.bad("Giao dịch chưa thành công");
        long remain = t.amount - t.refundedAmount, amt = r == null || r.amount() == null ? remain : r.amount();
        if (amt <= 0 || amt > remain) throw ApiException.bad("Số tiền hoàn không hợp lệ (tối đa " + remain + ")");
        String by = Current.user().email; Map<String, Object> res; boolean done;
        try {
            if ("VNPAY".equals(t.provider)) { res = gw.vnRefund(t.txnRef, amt, amt == t.amount, t.gatewayTxnNo, t.gwCreateDate, by, clientIp(rq)); done = Boolean.TRUE.equals(res.get("_signatureValid")) && "00".equals(res.get("vnp_ResponseCode")); }
            else { res = gw.momoRefund(t.txnRef + "-R" + (t.refundedAmount > 0 ? "2" : "1") + System.currentTimeMillis() % 100000, amt, t.gatewayTxnNo, "Hoan tien don " + t.orderCode); done = "0".equals(String.valueOf(res.get("resultCode"))); }
        } catch (IllegalStateException e) { throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY, e.getMessage()); }
        if (done) { t.refundedAmount += amt; t.note = "Refund " + amt + " by " + by; txns.save(t); }
        Map<String, Object> out = new LinkedHashMap<>(res); out.remove("signature"); out.remove("vnp_SecureHash"); out.put("refunded", done); out.put("refundedAmount", t.refundedAmount);
        return out;
    }
    /** Trạng thái cấu hình cổng (không lộ khoá). */
    @GetMapping("/api/admin/payments/gateways")
    public Map<String, Object> gateways() { return gw.status(sandbox); }
    @GetMapping("/api/public/payments/gateways")
    public Map<String, Object> publicGateways() { return Map.of("VNPAY", gw.vnpayReady() ? "REAL" : sandbox ? "MOCK" : "OFF", "MOMO", gw.momoReady() ? "REAL" : sandbox ? "MOCK" : "OFF"); }

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
