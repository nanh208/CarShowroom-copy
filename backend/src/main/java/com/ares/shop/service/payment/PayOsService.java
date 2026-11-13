package com.ares.shop.service.payment;

import com.ares.shop.model.CustomerOrder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * payOS (https://payos.vn/docs). Webhook body: {code, desc, success, data:{orderCode, amount, description, reference, ...}, signature}
 * signature = HMAC-SHA256(checksumKey, "k1=v1&k2=v2..." của data, key sắp xếp alphabet; null -> "").
 * payOS chỉ báo webhook cho link thanh toán tạo qua API => có hàm tạo link (description chứa mã đơn).
 */
@Component
public class PayOsService implements BankWebhookProvider {
    private final PaymentProps props; private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    public PayOsService(PaymentProps props) { this.props = props; }

    public String name() { return "PAYOS"; }
    public boolean enabled() { return props.payosEnabled && !PaymentProps.isBlank(props.payosChecksumKey); }

    public boolean verify(Map<String, String> headers, String rawBody) {
        try {
            JsonNode root = json.readTree(rawBody);
            JsonNode data = root.path("data");
            String sig = root.path("signature").asText("");
            if (!data.isObject() || sig.isBlank()) return false;
            return Hmac.equalsHex(signData(data, props.payosChecksumKey), sig);
        } catch (Exception e) { return false; }
    }

    /** Ký object data theo đúng quy tắc mẫu của payOS. */
    public static String signData(JsonNode data, String key) {
        return Hmac.sha256Hex(key, toQuery(data));
    }

    static String toQuery(JsonNode data) {
        TreeMap<String, JsonNode> sorted = new TreeMap<>();
        data.fields().forEachRemaining(e -> sorted.put(e.getKey(), e.getValue()));
        StringJoiner j = new StringJoiner("&");
        ObjectMapper m = new ObjectMapper();
        sorted.forEach((k, v) -> {
            String s;
            if (v == null || v.isNull() || v.isMissingNode()) s = "";
            else if (v.isArray()) {
                List<Object> arr = new ArrayList<>();
                v.forEach(el -> arr.add(el.isObject() ? CassoWebhook.sortDeep(m.convertValue(el, Map.class)) : m.convertValue(el, Object.class)));
                try { s = m.writeValueAsString(arr); } catch (Exception e) { s = ""; }
            } else if (v.isObject()) {
                try { s = m.writeValueAsString(CassoWebhook.sortDeep(m.convertValue(v, Map.class))); } catch (Exception e) { s = ""; }
            } else s = v.asText();
            if ("null".equals(s) || "undefined".equals(s)) s = "";
            j.add(k + "=" + s);
        });
        return j.toString();
    }

    public List<BankTransfer> parse(String rawBody) throws Exception {
        JsonNode root = json.readTree(rawBody);
        JsonNode d = root.path("data");
        boolean ok = "00".equals(root.path("code").asText()) && "00".equals(d.path("code").asText("00"));
        String id = d.path("reference").asText("");
        if (id.isBlank()) id = d.path("paymentLinkId").asText("") + ":" + d.path("orderCode").asText("");
        return List.of(new BankTransfer(name(), id, d.path("description").asText(""), d.path("amount").asLong(0), ok, d.path("accountNumber").asText("")));
    }

    /** Tạo link/QR payOS cho đơn. Trả về checkoutUrl + qrCode (chuỗi VietQR). */
    public Map<String, Object> createPaymentLink(CustomerOrder o, long amount, String returnUrl, String cancelUrl) throws Exception {
        long orderCode = Long.parseLong(String.valueOf(System.currentTimeMillis()).substring(3)) * 10 + (o.id % 10); // số nguyên duy nhất phía payOS
        String description = o.code; // <= 25 ký tự, chứa mã đơn để đối soát
        String raw = "amount=" + amount + "&cancelUrl=" + cancelUrl + "&description=" + description
                + "&orderCode=" + orderCode + "&returnUrl=" + returnUrl;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderCode", orderCode); body.put("amount", amount); body.put("description", description);
        body.put("cancelUrl", cancelUrl); body.put("returnUrl", returnUrl);
        body.put("signature", Hmac.sha256Hex(props.payosChecksumKey, raw));
        HttpRequest req = HttpRequest.newBuilder(URI.create(props.payosEndpoint + "/v2/payment-requests"))
                .header("Content-Type", "application/json").header("x-client-id", props.payosClientId).header("x-api-key", props.payosApiKey)
                .timeout(Duration.ofSeconds(8)).POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode n = json.readTree(res.body());
        if (!"00".equals(n.path("code").asText())) throw new IllegalStateException("payOS: " + n.path("desc").asText(res.body()));
        JsonNode d = n.path("data");
        // Xác thực chữ ký phản hồi nếu có
        String sig = n.path("signature").asText("");
        if (!sig.isBlank() && !Hmac.equalsHex(signData(d, props.payosChecksumKey), sig)) throw new IllegalStateException("payOS: sai chữ ký phản hồi");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("checkoutUrl", d.path("checkoutUrl").asText(""));
        out.put("qrCode", d.path("qrCode").asText(""));
        out.put("accountNumber", d.path("accountNumber").asText(""));
        out.put("accountName", d.path("accountName").asText(""));
        out.put("bin", d.path("bin").asText(""));
        out.put("amount", amount);
        out.put("description", description);
        return out;
    }
}
