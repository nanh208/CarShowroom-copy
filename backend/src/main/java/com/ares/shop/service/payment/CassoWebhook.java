package com.ares.shop.service.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Casso (https://developer.casso.vn).
 * - Webhook V2 (khuyên dùng): header "X-Casso-Signature: t=<ms>,v1=<hex>",
 *   chữ ký = HMAC-SHA512(checksumKey, t + "." + JSON(body đã sắp xếp key đệ quy)).
 * - Webhook V1 (cũ): header "secure-token" = token cấu hình. Body.data là mảng giao dịch.
 */
@Component
public class CassoWebhook implements BankWebhookProvider {
    private static final Pattern SIG = Pattern.compile("t=(\\d+),\\s*v1=([a-fA-F0-9]+)");
    private final PaymentProps props; private final ObjectMapper json = new ObjectMapper();
    /** Cho phép test cố định đồng hồ. */
    java.util.function.LongSupplier clock = System::currentTimeMillis;

    public CassoWebhook(PaymentProps props) { this.props = props; }

    public String name() { return "CASSO"; }
    public boolean enabled() {
        return props.cassoEnabled && (!PaymentProps.isBlank(props.cassoChecksumKey) || !PaymentProps.isBlank(props.cassoSecureToken));
    }

    public boolean verify(Map<String, String> headers, String rawBody) {
        String sig = headers.get("x-casso-signature");
        if (sig != null && !PaymentProps.isBlank(props.cassoChecksumKey)) {
            Matcher m = SIG.matcher(sig);
            if (!m.find()) return false;
            long ts = Long.parseLong(m.group(1));
            if (props.cassoMaxAgeSeconds > 0 && Math.abs(clock.getAsLong() - ts) > props.cassoMaxAgeSeconds * 1000) return false;
            try {
                return Hmac.equalsHex(signV2(ts, rawBody, props.cassoChecksumKey), m.group(2));
            } catch (Exception e) { return false; }
        }
        String token = headers.get("secure-token");
        if (token != null && !PaymentProps.isBlank(props.cassoSecureToken)) return Hmac.equalsSecret(props.cassoSecureToken, token);
        return false;
    }

    /** Tạo chữ ký V2 giống code mẫu chính thức của Casso. */
    public String signV2(long timestamp, String rawBody, String key) throws Exception {
        Map<String, Object> body = json.readValue(rawBody, new TypeReference<LinkedHashMap<String, Object>>() {});
        return Hmac.sha512Hex(key, timestamp + "." + json.writeValueAsString(sortDeep(body)));
    }

    @SuppressWarnings("unchecked")
    static Object sortDeep(Object v) {
        if (v instanceof Map<?, ?> m) {
            TreeMap<String, Object> t = new TreeMap<>();
            m.forEach((k, val) -> t.put(String.valueOf(k), sortDeep(val)));
            return t;
        }
        if (v instanceof List<?> l) { List<Object> out = new ArrayList<>(); l.forEach(x -> out.add(sortDeep(x))); return out; }
        return v;
    }

    public List<BankTransfer> parse(String rawBody) throws Exception {
        JsonNode root = json.readTree(rawBody);
        JsonNode data = root.path("data");
        List<BankTransfer> out = new ArrayList<>();
        if (data.isArray()) data.forEach(d -> out.add(one(d)));
        else if (data.isObject()) out.add(one(data));
        return out;
    }

    private BankTransfer one(JsonNode d) {
        // V2: id/reference/amount (âm = tiền ra); V1: id/tid/amount
        String id = d.path("id").asText("");
        if (id.isBlank() || "0".equals(id)) id = d.path("reference").asText(d.path("tid").asText(""));
        long amount = d.path("amount").asLong(0);
        String acc = d.path("accountNumber").asText(d.path("bank_sub_acc_id").asText(""));
        return new BankTransfer(name(), id, d.path("description").asText(""), Math.abs(amount), amount > 0, acc);
    }
}
