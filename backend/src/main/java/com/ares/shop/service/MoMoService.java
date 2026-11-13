package com.ares.shop.service;

import com.ares.shop.model.CustomerOrder;
import com.ares.shop.service.payment.Hmac;
import com.ares.shop.service.payment.PaymentProps;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Cổng MoMo (API v2, captureWallet). Đơn CHỈ được xác nhận qua IPN có chữ ký hợp lệ (xem verifyIpn).
 * Key lấy từ biến môi trường MOMO_PARTNER_CODE / MOMO_ACCESS_KEY / MOMO_SECRET_KEY.
 */
@Service
public class MoMoService {
    private final PaymentProps props;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public MoMoService(PaymentProps props) { this.props = props; }

    public boolean ready() { return props.momoReady(); }

    /** Chuỗi ký khi tạo giao dịch (theo tài liệu MoMo). */
    public static String createRawSignature(String accessKey, long amount, String extraData, String ipnUrl, String orderId, String orderInfo,
                                            String partnerCode, String redirectUrl, String requestId, String requestType) {
        return "accessKey=" + accessKey + "&amount=" + amount + "&extraData=" + extraData + "&ipnUrl=" + ipnUrl
                + "&orderId=" + orderId + "&orderInfo=" + orderInfo + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl + "&requestId=" + requestId + "&requestType=" + requestType;
    }

    /** Chuỗi ký của IPN / redirect (theo tài liệu MoMo). */
    public static String ipnRawSignature(String accessKey, Map<String, ?> p) {
        return "accessKey=" + accessKey + "&amount=" + s(p, "amount") + "&extraData=" + s(p, "extraData")
                + "&message=" + s(p, "message") + "&orderId=" + s(p, "orderId") + "&orderInfo=" + s(p, "orderInfo")
                + "&orderType=" + s(p, "orderType") + "&partnerCode=" + s(p, "partnerCode") + "&payType=" + s(p, "payType")
                + "&requestId=" + s(p, "requestId") + "&responseTime=" + s(p, "responseTime") + "&resultCode=" + s(p, "resultCode")
                + "&transId=" + s(p, "transId");
    }

    private static String s(Map<String, ?> p, String k) { Object v = p.get(k); return v == null ? "" : String.valueOf(v); }

    /** Xác thực chữ ký HMAC-SHA256 của IPN/redirect và partnerCode. */
    public boolean verifyIpn(Map<String, ?> p) {
        if (!ready()) return false;
        if (!props.momoPartnerCode.equals(s(p, "partnerCode"))) return false;
        String expected = Hmac.sha256Hex(props.momoSecretKey, ipnRawSignature(props.momoAccessKey, p));
        return Hmac.equalsHex(expected, s(p, "signature"));
    }

    /** Mã đơn nội bộ từ orderId MoMo dạng CODE_timestamp. */
    public static String orderCodeOf(String momoOrderId) {
        if (momoOrderId == null) return null;
        int i = momoOrderId.indexOf('_');
        return (i > 0 ? momoOrderId.substring(0, i) : momoOrderId).toUpperCase(Locale.ROOT);
    }

    /** Dùng URL mặc định: redirect về backend (chỉ hiển thị), IPN về /api/payment/momo/ipn. */
    public Map<String, Object> createPayment(CustomerOrder order, long amount) throws Exception {
        return createPayment(order, amount, props.backendBase() + "/api/payment/momo/return", props.backendBase() + "/api/payment/momo/ipn");
    }

    /** Tạo giao dịch trên cổng MoMo. Lỗi => ném exception (không còn link/QR giả). */
    public Map<String, Object> createPayment(CustomerOrder order, long amount, String redirectUrl, String ipnUrl) throws Exception {
        if (!ready()) throw new IllegalStateException("MoMo chưa được cấu hình");
        String orderId = order.code + "_" + System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        String orderInfo = "Thanh toan don hang " + order.code + " tai ARES Toys";
        String extraData = "";
        String requestType = "captureWallet";
        String raw = createRawSignature(props.momoAccessKey, amount, extraData, ipnUrl, orderId, orderInfo,
                props.momoPartnerCode, redirectUrl, requestId, requestType);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", props.momoPartnerCode);
        body.put("partnerName", "ARES Toys");
        body.put("storeId", "AresShowroom");
        body.put("requestId", requestId);
        body.put("amount", amount);
        body.put("orderId", orderId);
        body.put("orderInfo", orderInfo);
        body.put("redirectUrl", redirectUrl);
        body.put("ipnUrl", ipnUrl);
        body.put("lang", "vi");
        body.put("extraData", extraData);
        body.put("requestType", requestType);
        body.put("signature", Hmac.sha256Hex(props.momoSecretKey, raw));

        HttpRequest req = HttpRequest.newBuilder(URI.create(props.momoEndpoint + "/v2/gateway/api/create"))
                .header("Content-Type", "application/json").timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode n = json.readTree(res.body());
        if (n.path("resultCode").asInt(-1) != 0 || !n.hasNonNull("payUrl"))
            throw new IllegalStateException("MoMo: " + n.path("message").asText("không tạo được giao dịch"));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("payUrl", n.path("payUrl").asText());
        out.put("qrCodeUrl", n.path("qrCodeUrl").asText(""));
        out.put("deeplink", n.path("deeplink").asText(""));
        out.put("orderId", orderId);
        return out;
    }
}
