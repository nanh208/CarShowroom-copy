package com.ares.shop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Tích hợp cổng thật theo tài liệu chính thức:
 *  - VNPay vpcpay 2.1.0 (sandbox.vnpayment.vn/apis/docs): URL thanh toán ký HMAC-SHA512 trên chuỗi tham số sắp xếp a→z,
 *    giá trị URL-encode (US-ASCII, khoảng trắng = '+'); IPN/Return kiểm cùng cách; querydr/refund POST JSON ký chuỗi nối '|'.
 *  - MoMo v2 (developers.momo.vn): /v2/gateway/api/create (captureWallet|payWithATM), /query, /refund — ký HMAC-SHA256.
 * Không lưu/log khoá bí mật; trạng thái cấu hình chỉ trả về cờ + mã đối tác che bớt.
 */
@Service
public class GatewayClient {
    public static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final ObjectMapper om = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    public final String publicBase, vnTmn, vnSecret, vnPayUrl, vnApiUrl, momoPartner, momoAccess, momoSecret, momoEndpoint;
    public final boolean momoEnabled;

    public GatewayClient(@Value("${app.public-base-url}") String publicBase,
                         @Value("${app.payments.vnpay.tmn-code:}") String vnTmn, @Value("${app.payments.vnpay.hash-secret:}") String vnSecret,
                         @Value("${app.payments.vnpay.pay-url}") String vnPayUrl, @Value("${app.payments.vnpay.api-url}") String vnApiUrl,
                         @Value("${app.payments.momo.enabled:true}") boolean momoEnabled,
                         @Value("${app.payments.momo.partner-code:}") String momoPartner, @Value("${app.payments.momo.access-key:}") String momoAccess,
                         @Value("${app.payments.momo.secret-key:}") String momoSecret, @Value("${app.payments.momo.endpoint}") String momoEndpoint) {
        this.publicBase = publicBase.replaceAll("/+$", ""); this.vnTmn = vnTmn.trim(); this.vnSecret = vnSecret.trim(); this.vnPayUrl = vnPayUrl; this.vnApiUrl = vnApiUrl;
        this.momoEnabled = momoEnabled; this.momoPartner = momoPartner.trim(); this.momoAccess = momoAccess.trim(); this.momoSecret = momoSecret.trim(); this.momoEndpoint = momoEndpoint.replaceAll("/+$", "");
    }
    public boolean vnpayReady() { return !vnTmn.isEmpty() && !vnSecret.isEmpty(); }
    public boolean momoReady() { return momoEnabled && !momoPartner.isEmpty() && !momoAccess.isEmpty() && !momoSecret.isEmpty(); }
    public boolean momoIsPublicTestKey() { return "F8BBA842ECF85".equals(momoAccess); }
    public String vnReturnUrl() { return publicBase + "/pay/return/vnpay"; }
    public String vnIpnUrl() { return publicBase + "/api/public/payments/vnpay/ipn"; }
    public String momoReturnUrl() { return publicBase + "/pay/return/momo"; }
    public String momoIpnUrl() { return publicBase + "/api/public/payments/momo/ipn"; }
    public static String nowVn() { return ZonedDateTime.now(VN).format(F); }

    public static String hmacHex(String alg, String key, String data) {
        try { Mac m = Mac.getInstance(alg); m.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), alg));
            StringBuilder sb = new StringBuilder(); for (byte b : m.doFinal(data.getBytes(StandardCharsets.UTF_8))) sb.append(String.format("%02x", b)); return sb.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    public static boolean eq(String a, String b) { return a != null && b != null && MessageDigest.isEqual(a.toLowerCase().getBytes(StandardCharsets.UTF_8), b.toLowerCase().getBytes(StandardCharsets.UTF_8)); }
    static String enc(String v) { return URLEncoder.encode(v, StandardCharsets.US_ASCII); }

    // ---------- VNPay ----------
    /** Chuỗi băm VNPay: key=urlencode(value) sắp xếp theo key, bỏ vnp_SecureHash/vnp_SecureHashType và giá trị rỗng. */
    public static String vnCanonical(Map<String, String> p) {
        StringBuilder sb = new StringBuilder();
        new TreeMap<>(p).forEach((k, v) -> { if (!k.startsWith("vnp_") || k.equals("vnp_SecureHash") || k.equals("vnp_SecureHashType") || v == null || v.isEmpty()) return;
            if (sb.length() > 0) sb.append('&'); sb.append(enc(k)).append('=').append(enc(v)); });
        return sb.toString();
    }
    public String vnPayUrl(String txnRef, long amount, String orderInfo, String ip, String createDate, String expireDate, String locale, String bankCode) {
        Map<String, String> p = new TreeMap<>();
        p.put("vnp_Version", "2.1.0"); p.put("vnp_Command", "pay"); p.put("vnp_TmnCode", vnTmn); p.put("vnp_Amount", String.valueOf(amount * 100));
        p.put("vnp_CurrCode", "VND"); p.put("vnp_TxnRef", txnRef); p.put("vnp_OrderInfo", orderInfo); p.put("vnp_OrderType", "other");
        p.put("vnp_Locale", "en".equals(locale) ? "en" : "vn"); p.put("vnp_ReturnUrl", vnReturnUrl()); p.put("vnp_IpAddr", ip == null || ip.isBlank() ? "127.0.0.1" : ip);
        p.put("vnp_CreateDate", createDate); p.put("vnp_ExpireDate", expireDate);
        if (bankCode != null && !bankCode.isBlank()) p.put("vnp_BankCode", bankCode);
        String q = vnCanonical(p);
        return vnPayUrl + "?" + q + "&vnp_SecureHash=" + hmacHex("HmacSHA512", vnSecret, q);
    }
    public boolean vnVerify(Map<String, String> p) { return vnpayReady() && eq(p.get("vnp_SecureHash"), hmacHex("HmacSHA512", vnSecret, vnCanonical(p))); }

    private Map<String, Object> postJson(String url, Map<String, Object> body) {
        try {
            HttpRequest rq = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body))).build();
            HttpResponse<String> r = http.send(rq, HttpResponse.BodyHandlers.ofString());
            @SuppressWarnings("unchecked") Map<String, Object> m = om.readValue(r.body(), Map.class);
            return m;
        } catch (Exception e) { throw new IllegalStateException("Không gọi được cổng thanh toán: " + e.getClass().getSimpleName() + " " + Objects.toString(e.getMessage(), "")); }
    }
    static String s(Object o) { return o == null ? "" : String.valueOf(o); }

    /** querydr: hash = RequestId|Version|Command|TmnCode|TxnRef|TransactionDate|CreateDate|IpAddr|OrderInfo. Kiểm chữ ký phản hồi. */
    public Map<String, Object> vnQuery(String txnRef, String transactionDate, String ip) {
        String rid = UUID.randomUUID().toString().replace("-", ""), cd = nowVn(), info = "Truy van GD " + txnRef;
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("vnp_RequestId", rid); b.put("vnp_Version", "2.1.0"); b.put("vnp_Command", "querydr"); b.put("vnp_TmnCode", vnTmn); b.put("vnp_TxnRef", txnRef);
        b.put("vnp_OrderInfo", info); b.put("vnp_TransactionDate", transactionDate); b.put("vnp_CreateDate", cd); b.put("vnp_IpAddr", ip);
        b.put("vnp_SecureHash", hmacHex("HmacSHA512", vnSecret, String.join("|", rid, "2.1.0", "querydr", vnTmn, txnRef, transactionDate, cd, ip, info)));
        Map<String, Object> r = postJson(vnApiUrl, b);
        String data = String.join("|", s(r.get("vnp_ResponseId")), s(r.get("vnp_Command")), s(r.get("vnp_ResponseCode")), s(r.get("vnp_Message")), s(r.get("vnp_TmnCode")), s(r.get("vnp_TxnRef")),
            s(r.get("vnp_Amount")), s(r.get("vnp_BankCode")), s(r.get("vnp_PayDate")), s(r.get("vnp_TransactionNo")), s(r.get("vnp_TransactionType")), s(r.get("vnp_TransactionStatus")),
            s(r.get("vnp_OrderInfo")), s(r.get("vnp_PromotionCode")), s(r.get("vnp_PromotionAmount")));
        r.put("_signatureValid", eq(s(r.get("vnp_SecureHash")), hmacHex("HmacSHA512", vnSecret, data)));
        return r;
    }
    /** refund: hash = RequestId|Version|Command|TmnCode|TransactionType|TxnRef|Amount|TransactionNo|TransactionDate|CreateBy|CreateDate|IpAddr|OrderInfo. */
    public Map<String, Object> vnRefund(String txnRef, long amount, boolean full, String transactionNo, String transactionDate, String by, String ip) {
        String rid = UUID.randomUUID().toString().replace("-", ""), cd = nowVn(), info = "Hoan tien GD " + txnRef, type = full ? "02" : "03", amt = String.valueOf(amount * 100), no = transactionNo == null ? "" : transactionNo;
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("vnp_RequestId", rid); b.put("vnp_Version", "2.1.0"); b.put("vnp_Command", "refund"); b.put("vnp_TmnCode", vnTmn); b.put("vnp_TransactionType", type);
        b.put("vnp_TxnRef", txnRef); b.put("vnp_Amount", amt); b.put("vnp_OrderInfo", info); b.put("vnp_TransactionNo", no); b.put("vnp_TransactionDate", transactionDate);
        b.put("vnp_CreateBy", by); b.put("vnp_CreateDate", cd); b.put("vnp_IpAddr", ip);
        b.put("vnp_SecureHash", hmacHex("HmacSHA512", vnSecret, String.join("|", rid, "2.1.0", "refund", vnTmn, type, txnRef, amt, no, transactionDate, by, cd, ip, info)));
        Map<String, Object> r = postJson(vnApiUrl, b);
        String data = String.join("|", s(r.get("vnp_ResponseId")), s(r.get("vnp_Command")), s(r.get("vnp_ResponseCode")), s(r.get("vnp_Message")), s(r.get("vnp_TmnCode")), s(r.get("vnp_TxnRef")),
            s(r.get("vnp_Amount")), s(r.get("vnp_BankCode")), s(r.get("vnp_PayDate")), s(r.get("vnp_TransactionNo")), s(r.get("vnp_TransactionType")), s(r.get("vnp_TransactionStatus")), s(r.get("vnp_OrderInfo")));
        r.put("_signatureValid", eq(s(r.get("vnp_SecureHash")), hmacHex("HmacSHA512", vnSecret, data)));
        return r;
    }

    // ---------- MoMo ----------
    String momoSign(String raw) { return hmacHex("HmacSHA256", momoSecret, raw); }
    /** create: chữ ký accessKey&amount&extraData&ipnUrl&orderId&orderInfo&partnerCode&redirectUrl&requestId&requestType. */
    public Map<String, Object> momoCreate(String orderId, String requestId, long amount, String orderInfo, String requestType, String lang) {
        String extra = "", rt = "payWithATM".equals(requestType) ? "payWithATM" : "captureWallet";
        String raw = "accessKey=" + momoAccess + "&amount=" + amount + "&extraData=" + extra + "&ipnUrl=" + momoIpnUrl() + "&orderId=" + orderId + "&orderInfo=" + orderInfo
            + "&partnerCode=" + momoPartner + "&redirectUrl=" + momoReturnUrl() + "&requestId=" + requestId + "&requestType=" + rt;
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("partnerCode", momoPartner); b.put("storeName", "ARES Toys"); b.put("storeId", "ARES"); b.put("requestId", requestId); b.put("amount", amount); b.put("orderId", orderId);
        b.put("orderInfo", orderInfo); b.put("redirectUrl", momoReturnUrl()); b.put("ipnUrl", momoIpnUrl()); b.put("lang", "en".equals(lang) ? "en" : "vi"); b.put("requestType", rt);
        b.put("autoCapture", true); b.put("extraData", extra); b.put("signature", momoSign(raw));
        Map<String, Object> r = postJson(momoEndpoint + "/v2/gateway/api/create", b);
        if (r.get("signature") != null) { // phản hồi: accessKey&amount&message&orderId&partnerCode&payUrl&requestId&responseTime&resultCode
            String rr = "accessKey=" + momoAccess + "&amount=" + s(r.get("amount")) + "&message=" + s(r.get("message")) + "&orderId=" + s(r.get("orderId")) + "&partnerCode=" + s(r.get("partnerCode"))
                + "&payUrl=" + s(r.get("payUrl")) + "&requestId=" + s(r.get("requestId")) + "&responseTime=" + s(r.get("responseTime")) + "&resultCode=" + s(r.get("resultCode"));
            r.put("_signatureValid", eq(s(r.get("signature")), momoSign(rr)));
        }
        return r;
    }
    /** IPN/redirect: accessKey&amount&extraData&message&orderId&orderInfo&orderType&partnerCode&payType&requestId&responseTime&resultCode&transId. */
    public boolean momoVerify(Map<String, ?> p) {
        if (!momoReady()) return false;
        String raw = "accessKey=" + momoAccess + "&amount=" + s(p.get("amount")) + "&extraData=" + s(p.get("extraData")) + "&message=" + s(p.get("message")) + "&orderId=" + s(p.get("orderId"))
            + "&orderInfo=" + s(p.get("orderInfo")) + "&orderType=" + s(p.get("orderType")) + "&partnerCode=" + s(p.get("partnerCode")) + "&payType=" + s(p.get("payType"))
            + "&requestId=" + s(p.get("requestId")) + "&responseTime=" + s(p.get("responseTime")) + "&resultCode=" + s(p.get("resultCode")) + "&transId=" + s(p.get("transId"));
        return eq(s(p.get("signature")), momoSign(raw));
    }
    /** Dùng cho kiểm thử: ký một payload IPN như MoMo. */
    public String momoSignIpn(Map<String, ?> p) {
        return momoSign("accessKey=" + momoAccess + "&amount=" + s(p.get("amount")) + "&extraData=" + s(p.get("extraData")) + "&message=" + s(p.get("message")) + "&orderId=" + s(p.get("orderId"))
            + "&orderInfo=" + s(p.get("orderInfo")) + "&orderType=" + s(p.get("orderType")) + "&partnerCode=" + s(p.get("partnerCode")) + "&payType=" + s(p.get("payType"))
            + "&requestId=" + s(p.get("requestId")) + "&responseTime=" + s(p.get("responseTime")) + "&resultCode=" + s(p.get("resultCode")) + "&transId=" + s(p.get("transId")));
    }
    /** query: accessKey&orderId&partnerCode&requestId. */
    public Map<String, Object> momoQuery(String orderId) {
        String rid = orderId + "-Q" + System.currentTimeMillis();
        Map<String, Object> b = new LinkedHashMap<>(Map.of("partnerCode", momoPartner, "requestId", rid, "orderId", orderId, "lang", "vi"));
        b.put("signature", momoSign("accessKey=" + momoAccess + "&orderId=" + orderId + "&partnerCode=" + momoPartner + "&requestId=" + rid));
        return postJson(momoEndpoint + "/v2/gateway/api/query", b);
    }
    /** refund: accessKey&amount&description&orderId&partnerCode&requestId&transId. */
    public Map<String, Object> momoRefund(String refundOrderId, long amount, String transId, String description) {
        String rid = refundOrderId;
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("partnerCode", momoPartner); b.put("orderId", refundOrderId); b.put("requestId", rid); b.put("amount", amount); b.put("transId", Long.parseLong(transId)); b.put("lang", "vi"); b.put("description", description);
        b.put("signature", momoSign("accessKey=" + momoAccess + "&amount=" + amount + "&description=" + description + "&orderId=" + refundOrderId + "&partnerCode=" + momoPartner + "&requestId=" + rid + "&transId=" + transId));
        return postJson(momoEndpoint + "/v2/gateway/api/refund", b);
    }

    /** Trạng thái cấu hình cho trang admin — KHÔNG trả khoá bí mật. */
    public Map<String, Object> status(boolean sandboxEnabled) {
        Map<String, Object> vn = new LinkedHashMap<>();
        vn.put("mode", vnpayReady() ? "REAL" : (sandboxEnabled ? "MOCK" : "DISABLED")); vn.put("tmnCodeSet", !vnTmn.isEmpty()); vn.put("hashSecretSet", !vnSecret.isEmpty());
        vn.put("tmnCode", mask(vnTmn)); vn.put("payUrl", vnPayUrl); vn.put("apiUrl", vnApiUrl); vn.put("sandboxEndpoint", vnPayUrl.contains("sandbox"));
        vn.put("returnUrl", vnReturnUrl()); vn.put("ipnUrl", vnIpnUrl());
        Map<String, Object> mo = new LinkedHashMap<>();
        mo.put("mode", momoReady() ? "REAL" : (sandboxEnabled ? "MOCK" : "DISABLED")); mo.put("partnerCode", mask(momoPartner)); mo.put("accessKeySet", !momoAccess.isEmpty()); mo.put("secretKeySet", !momoSecret.isEmpty());
        mo.put("publicTestKeys", momoIsPublicTestKey()); mo.put("endpoint", momoEndpoint); mo.put("sandboxEndpoint", momoEndpoint.contains("test-"));
        mo.put("returnUrl", momoReturnUrl()); mo.put("ipnUrl", momoIpnUrl());
        boolean pub = publicBase.startsWith("https://") && !publicBase.contains("localhost") && !publicBase.contains("127.0.0.1");
        return Map.of("vnpay", vn, "momo", mo, "publicBaseUrl", publicBase, "publicBaseReachable", pub, "sandboxEnabled", sandboxEnabled);
    }
    static String mask(String v) { return v == null || v.isEmpty() ? "" : v.length() <= 4 ? "****" : v.substring(0, 2) + "****" + v.substring(v.length() - 2); }
}
