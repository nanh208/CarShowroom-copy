package com.ares.shop.service;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/**
 * Trợ lý AI qua API tương thích OpenAI (/v1/chat/completions + tools). RAG: mô hình gọi công cụ, server tự tra
 * catalog/FAQ/mã giảm giá/đơn hàng (kiểm quyền: đơn chỉ lộ chi tiết khi là chủ đơn hoặc khớp SĐT). Không có AI_API_KEY → tắt (bot từ khoá).
 */
@Service
public class AiService {
    private final ObjectMapper om = new ObjectMapper().findAndRegisterModules().disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    public final String key, base, model; final int timeoutS;
    private final ProductRepo products; private final ChatIntentRepo intents; private final PromotionRepo promos; private final OrderRepo orders; private final SearchService search;

    public AiService(@Value("${app.ai.api-key:}") String key, @Value("${app.ai.base-url:https://api.openai.com/v1}") String base, @Value("${app.ai.model:gpt-4o-mini}") String model,
                     @Value("${app.ai.timeout-seconds:25}") int timeoutS, ProductRepo products, ChatIntentRepo intents, PromotionRepo promos, OrderRepo orders, SearchService search) {
        this.key = key.trim(); this.base = base.replaceAll("/+$", ""); this.model = model; this.timeoutS = timeoutS;
        this.products = products; this.intents = intents; this.promos = promos; this.orders = orders; this.search = search;
    }
    public boolean enabled() { return !key.isEmpty(); }
    public Map<String, Object> status() { return Map.of("enabled", enabled(), "baseUrl", base, "model", model, "keySet", enabled()); }

    static final String SYSTEM = """
        Bạn là trợ lý bán hàng của ARES Toys (xe mô hình, xe RC, phụ kiện) tại Việt Nam. Trả lời ngắn gọn, thân thiện, đúng ngôn ngữ của khách (%s).
        CHỈ dùng dữ liệu từ công cụ; không bịa giá, tồn kho, chính sách hay trạng thái đơn. Giá tính bằng VND.
        Dùng search_products khi khách hỏi/tìm sản phẩm; faq cho chính sách (ship, đổi trả, bảo hành, thanh toán…); coupons cho mã giảm giá;
        order_status khi có mã đơn (cần SĐT nếu khách chưa đăng nhập). Nếu không chắc hoặc khách muốn gặp người, đề nghị "Chuyển nhân viên".
        Không thực hiện thanh toán, huỷ đơn hay thay đổi dữ liệu.""";

    static List<Map<String, Object>> tools() {
        return List.of(
            fn("search_products", "Tìm sản phẩm trong catalog theo từ khoá/hãng/tỉ lệ/ngân sách (tiếng Việt hoặc Anh).", Map.of("query", Map.of("type", "string")), List.of("query")),
            fn("faq", "Tra câu trả lời chính sách/FAQ của cửa hàng.", Map.of("topic", Map.of("type", "string")), List.of("topic")),
            fn("coupons", "Liệt kê mã giảm giá công khai đang hiệu lực.", Map.of(), List.of()),
            fn("order_status", "Tra trạng thái đơn hàng theo mã đơn (+ SĐT đặt hàng nếu có).", Map.of("code", Map.of("type", "string"), "phone", Map.of("type", "string")), List.of("code")));
    }
    static Map<String, Object> fn(String name, String desc, Map<String, Object> props, List<String> req) {
        return Map.of("type", "function", "function", Map.of("name", name, "description", desc, "parameters", Map.of("type", "object", "properties", props, "required", req)));
    }

    public record Reply(String text, List<Map<String, Object>> products, List<Map<String, Object>> coupons, Map<String, Object> order, List<String> toolsUsed) {}

    /** history: các cặp [hỏi, đáp] gần nhất của phiên. */
    public Reply chat(String question, List<String[]> history, String lang, User u) throws Exception {
        List<Map<String, Object>> msgs = new ArrayList<>();
        msgs.add(Map.of("role", "system", "content", SYSTEM.formatted("en".equals(lang) ? "English" : "tiếng Việt")));
        for (String[] h : history) { msgs.add(Map.of("role", "user", "content", h[0])); if (h[1] != null) msgs.add(Map.of("role", "assistant", "content", h[1])); }
        msgs.add(Map.of("role", "user", "content", question));
        List<Map<String, Object>> prods = new ArrayList<>(), coups = new ArrayList<>(); Map<String, Object> order = null; List<String> used = new ArrayList<>();
        for (int round = 0; round < 4; round++) {
            Map<String, Object> body = new LinkedHashMap<>(Map.of("model", model, "messages", msgs, "temperature", 0.3, "max_tokens", 500));
            if (round < 3) { body.put("tools", tools()); body.put("tool_choice", "auto"); }
            HttpRequest rq = HttpRequest.newBuilder(URI.create(base + "/chat/completions")).timeout(Duration.ofSeconds(timeoutS))
                .header("Authorization", "Bearer " + key).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body))).build();
            HttpResponse<String> r = http.send(rq, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() / 100 != 2) throw new IllegalStateException("LLM HTTP " + r.statusCode());
            @SuppressWarnings("unchecked") Map<String, Object> msg = (Map<String, Object>) ((Map<String, Object>) ((List<?>) om.readValue(r.body(), Map.class).get("choices")).get(0)).get("message");
            List<?> calls = (List<?>) msg.get("tool_calls");
            if (calls == null || calls.isEmpty()) {
                String text = Objects.toString(msg.get("content"), "").trim();
                if (text.isEmpty()) throw new IllegalStateException("LLM empty");
                return new Reply(text, prods, coups, order, used);
            }
            Map<String, Object> am = new LinkedHashMap<>(); am.put("role", "assistant"); am.put("content", msg.get("content")); am.put("tool_calls", calls); msgs.add(am);
            for (Object c : calls) {
                @SuppressWarnings("unchecked") Map<String, Object> call = (Map<String, Object>) c; @SuppressWarnings("unchecked") Map<String, Object> f = (Map<String, Object>) call.get("function");
                String name = String.valueOf(f.get("name")); used.add(name);
                Map<?, ?> args; try { args = om.readValue(String.valueOf(f.getOrDefault("arguments", "{}")), Map.class); } catch (Exception e) { args = Map.of(); }
                Object result;
                switch (name) {
                    case "search_products" -> { var l = searchProducts(String.valueOf(args.get("query"))); l.forEach(p -> { if (prods.stream().noneMatch(x -> x.get("id").equals(p.get("id"))) && prods.size() < 6) prods.add(p); }); result = l; }
                    case "faq" -> result = faq(String.valueOf(args.get("topic")), lang);
                    case "coupons" -> { var l = coupons(); coups.clear(); coups.addAll(l); result = l; }
                    case "order_status" -> { var o = orderStatus(String.valueOf(args.get("code")), args.get("phone") == null ? null : String.valueOf(args.get("phone")), u); if (o.containsKey("code")) order = o; result = o; }
                    default -> result = Map.of("error", "unknown tool");
                }
                msgs.add(Map.of("role", "tool", "tool_call_id", String.valueOf(call.get("id")), "content", om.writeValueAsString(result)));
            }
        }
        throw new IllegalStateException("LLM tool loop");
    }

    /** Mô tả ảnh thành từ khoá tìm kiếm (model hỗ trợ ảnh, vd. gpt-4o-mini). */
    public String describeImage(byte[] img, String mime) throws Exception {
        String data = "data:" + (mime == null ? "image/jpeg" : mime) + ";base64," + Base64.getEncoder().encodeToString(img);
        Map<String, Object> body = Map.of("model", model, "max_tokens", 40, "temperature", 0, "messages", List.of(Map.of("role", "user", "content", List.of(
            Map.of("type", "text", "text", "Ảnh này là xe mô hình/đồ chơi gì? Trả về 3-6 từ khoá tìm kiếm (hãng xe, mẫu xe, loại: xe RC/mô hình/lego, màu), cách nhau bởi dấu cách, không giải thích."),
            Map.of("type", "image_url", "image_url", Map.of("url", data))))));
        HttpRequest rq = HttpRequest.newBuilder(URI.create(base + "/chat/completions")).timeout(Duration.ofSeconds(timeoutS)).header("Authorization", "Bearer " + key)
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body))).build();
        HttpResponse<String> r = http.send(rq, HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() / 100 != 2) return null;
        @SuppressWarnings("unchecked") Map<String, Object> msg = (Map<String, Object>) ((Map<String, Object>) ((List<?>) om.readValue(r.body(), Map.class).get("choices")).get(0)).get("message");
        return Objects.toString(msg.get("content"), "").replaceAll("[\\r\\n,]+", " ").trim();
    }

    List<Map<String, Object>> searchProducts(String q) {
        return search.search(q == null ? "" : q, true, false).stream().limit(5).map(p -> {
            Map<String, Object> m = new LinkedHashMap<>(); m.put("id", p.id); m.put("name", p.name); m.put("price", p.price); m.put("stock", p.stock); m.put("scale", p.scale);
            m.put("brand", p.brand == null ? null : p.brand.name); m.put("rating", p.rating); m.put("image", p.images.isEmpty() ? "" : p.images.get(0)); m.put("url", "/products/" + p.id); return m; }).toList();
    }
    List<Map<String, Object>> faq(String topic, String lang) {
        String n = com.ares.shop.web.BotController.norm(topic);
        List<Map<String, Object>> out = new ArrayList<>();
        for (ChatIntent it : intents.findAllByOrderBySortOrderAscIdAsc()) {
            if (!Boolean.TRUE.equals(it.active)) continue;
            String hay = com.ares.shop.web.BotController.norm(it.name + " " + it.keywords);
            int sc = 0; for (String w : n.split(" ")) if (w.length() > 2 && hay.contains(w)) sc++;
            if (sc > 0) out.add(Map.of("topic", it.name, "score", sc, "answer", "en".equals(lang) && it.answerEn != null && !it.answerEn.isBlank() ? it.answerEn : it.answer));
        }
        out.sort((a, b) -> (int) b.get("score") - (int) a.get("score"));
        return out.stream().limit(3).toList();
    }
    List<Map<String, Object>> coupons() {
        return promos.findByActiveTrue().stream().filter(p -> p.isPublic && p.isValidNow()).limit(5).map(p -> {
            Map<String, Object> m = new LinkedHashMap<>(); m.put("code", p.code); m.put("title", p.title); m.put("minOrder", p.minOrder); m.put("endDate", p.endDate);
            m.put("value", p.kind == Enums.VoucherKind.PERCENT ? p.discountPercent + "%" : MailService.vnd(p.amount)); return m; }).toList();
    }
    Map<String, Object> orderStatus(String code, String phone, User u) {
        var o = orders.findByCode(code == null ? "" : code.trim().toUpperCase()).orElse(null);
        if (o == null) return Map.of("error", "not found");
        boolean mine = u != null && o.user != null && o.user.id.equals(u.id), phoneOk = phone != null && phone.replaceAll("\\D", "").equals(o.phone);
        Map<String, Object> m = new LinkedHashMap<>(); m.put("code", o.code); m.put("status", o.status); m.put("paymentStatus", o.paymentStatus); m.put("etaFrom", o.etaFrom); m.put("etaTo", o.etaTo); m.put("createdAt", o.createdAt);
        if (mine || phoneOk) { m.put("total", o.total); m.put("items", o.items.stream().map(i -> i.quantity + " × " + i.productName).toList()); }
        else m.put("note", "Chi tiết cần đăng nhập hoặc SĐT đặt hàng");
        return m;
    }
}
