package com.ares.shop.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * SePay (https://docs.sepay.vn): webhook gửi header "Authorization: Apikey <API_KEY>".
 * Body: {id, gateway, transactionDate, accountNumber, content, transferType: "in"|"out", transferAmount, referenceCode, ...}
 */
@Component
public class SePayWebhook implements BankWebhookProvider {
    private final PaymentProps props; private final ObjectMapper json = new ObjectMapper();
    public SePayWebhook(PaymentProps props) { this.props = props; }

    public String name() { return "SEPAY"; }
    public boolean enabled() { return props.sepayEnabled && !PaymentProps.isBlank(props.sepayApiKey); }

    public boolean verify(Map<String, String> headers, String rawBody) {
        String auth = headers.get("authorization");
        if (auth == null) return false;
        auth = auth.trim();
        if (!auth.regionMatches(true, 0, "Apikey ", 0, 7)) return false;
        return Hmac.equalsSecret(props.sepayApiKey, auth.substring(7));
    }

    public List<BankTransfer> parse(String rawBody) throws Exception {
        JsonNode n = json.readTree(rawBody);
        String id = n.path("id").asText("");
        String content = n.path("content").asText("");
        if (content.isBlank()) content = n.path("description").asText("");
        String code = n.path("code").asText("");           // SePay có thể tự nhận diện "mã thanh toán"
        if (!code.isBlank() && !"null".equals(code)) content = code + " " + content;
        boolean in = "in".equalsIgnoreCase(n.path("transferType").asText(""));
        long amount = n.path("transferAmount").asLong(0);
        return List.of(new BankTransfer(name(), id, content, amount, in, n.path("accountNumber").asText("")));
    }
}
