package com.ares.shop.service;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Dich vu tu van AI thong minh chuyen sau ve xe mo hinh tinh Diecast va xe dieu khien RC.
 * Ho tro ket noi truc tiep Google Gemini API voi catalog-awareness (RAG)
 * va he thong tri thuc noi bo offline du phong (Ares Knowledge Engine) de luon phan hoi nhanh, chuan xac.
 */
@Service
public class AiConsultantService {
    private static final Logger log = LoggerFactory.getLogger(AiConsultantService.class);

    private final SettingRepo settingRepo;
    private final ProductRepo productRepo;
    private final PromotionRepo promoRepo;
    private final ObjectMapper mapper;
    private final HttpClient http;

    @Value("${ares.ai.gemini-api-key:}")
    private String defaultApiKey;

    private static final String DEFAULT_MODEL = "gemini-1.5-flash";
    private static final String DEFAULT_SYSTEM_PROMPT =
            "Ban la chuyen gia tu van dam me xe mo hinh va xe dieu khien RC tai ARES Toys (ares.vn). "
            + "Nhiem vu cua ban la tu van nhiet tinh, am hieu sau ve cac dong xe, ty le (1:64 bo tui sieu chi tiet, 1:43 trung binh, 1:24 mo cua, 1:18 mo full chi tiet may va noi that, 1:10 RC co lon), "
            + "chat lieu hop kim duc Zamak chong ri, son tinh dien, xe RC drift, buggy, crawler, pin LiPo va cach bao quan mo hinh. "
            + "Hay tra loi bang tieng Viet tu nhien, than thien, chuyen nghiep, ngan gon ro rang. Neu co san pham phu hop trong danh muc cua cua hang, hay chu dong nhac den ten xe va ly do nen chon.";

    public record AiResult(String text, List<Map<String, Object>> products, List<String> quick, boolean aiPowered, String modelUsed) {}

    public AiConsultantService(SettingRepo settingRepo, ProductRepo productRepo, PromotionRepo promoRepo, ObjectMapper mapper) {
        this.settingRepo = settingRepo;
        this.productRepo = productRepo;
        this.promoRepo = promoRepo;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
    }

    public static String norm(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9: ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public boolean isEnabled() {
        return "true".equalsIgnoreCase(getSetting("ai_enabled", "true"));
    }

    public String getApiKey() {
        String k = getSetting("ai_api_key", "");
        if (k.isBlank()) k = Optional.ofNullable(defaultApiKey).orElse("");
        if (k.isBlank()) k = Optional.ofNullable(System.getenv("GEMINI_API_KEY")).orElse("");
        return k.trim();
    }

    public String getModel() {
        return getSetting("ai_model", DEFAULT_MODEL);
    }

    public String getSystemPrompt() {
        return getSetting("ai_system_prompt", DEFAULT_SYSTEM_PROMPT);
    }

    private String getSetting(String key, String def) {
        return settingRepo.findById(key).map(s -> s.v).filter(v -> v != null && !v.isBlank()).orElse(def);
    }

    private void setSetting(String key, String val) {
        AppSetting s = new AppSetting();
        s.k = key;
        s.v = val;
        settingRepo.save(s);
    }

    public Map<String, Object> getConfig() {
        String key = getApiKey();
        String masked = key.length() > 8 ? key.substring(0, 4) + "..." + key.substring(key.length() - 4) : (key.isBlank() ? "" : "****");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabled", isEnabled());
        m.put("provider", "Google Gemini");
        m.put("model", getModel());
        m.put("hasApiKey", !key.isBlank());
        m.put("maskedApiKey", masked);
        m.put("systemPrompt", getSystemPrompt());
        return m;
    }

    public void saveConfig(Map<String, Object> req) {
        if (req.containsKey("enabled")) {
            setSetting("ai_enabled", String.valueOf(req.get("enabled")));
        }
        if (req.containsKey("apiKey")) {
            String k = String.valueOf(req.get("apiKey")).trim();
            if (!k.contains("...")) {
                setSetting("ai_api_key", k);
            }
        }
        if (req.containsKey("model")) {
            setSetting("ai_model", String.valueOf(req.get("model")).trim());
        }
        if (req.containsKey("systemPrompt")) {
            setSetting("ai_system_prompt", String.valueOf(req.get("systemPrompt")).trim());
        }
    }

    /**
     * Xu ly cau hoi tu van cua khach hang.
     */
    public AiResult consult(String query, User user, String session) {
        String q = Optional.ofNullable(query).orElse("").trim();
        List<Map<String, Object>> matchedProducts = findRelevantProducts(q);
        List<String> dynamicQuick = buildFollowUpQuick(q);

        String apiKey = getApiKey();
        if (isEnabled() && !apiKey.isBlank()) {
            try {
                String aiText = callGeminiApi(q, apiKey, getModel(), matchedProducts);
                if (aiText != null && !aiText.isBlank()) {
                    return new AiResult(aiText.trim(), matchedProducts, dynamicQuick, true, getModel());
                }
            } catch (Exception e) {
                log.warn("Gemini API call failed or timed out, fallback to Ares Knowledge Engine: {}", e.getMessage());
            }
        }

        // Fallback: Engine tri thuc chuyen gia noi bo Ares
        String offlineText = buildExpertConsultation(q, matchedProducts);
        return new AiResult(offlineText, matchedProducts, dynamicQuick, true, "Ares Diecast Knowledge Engine");
    }

    private String callGeminiApi(String userQuery, String apiKey, String model, List<Map<String, Object>> relatedProducts) throws Exception {
        String catalogContext = buildCatalogSummary(relatedProducts);
        String fullSystemPrompt = getSystemPrompt() + "\n\n[Thong tin san pham lien quan trong kho cua Ares Toys hien co]:\n" + catalogContext;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("system_instruction", Map.of("parts", List.of(Map.of("text", fullSystemPrompt))));
        payload.put("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userQuery)))));
        payload.put("generationConfig", Map.of("temperature", 0.7, "maxOutputTokens", 800));

        String jsonBody = mapper.writeValueAsString(payload);
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(8))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode root = mapper.readTree(response.body());
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    return parts.get(0).path("text").asText();
                }
            }
        } else {
            log.warn("Gemini API returned status code {}: {}", response.statusCode(), response.body());
        }
        return null;
    }

    private String buildCatalogSummary(List<Map<String, Object>> products) {
        if (products == null || products.isEmpty()) {
            return "Cua hang co san hon 40 mau xe mo hinh tinh diecast (Hot Wheels, Mini GT, Tomica, Bburago, Maisto) va xe RC dieu khien toc do cao. Gia tu 189.000d den hon 3.000.000d.";
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> p : products) {
            sb.append("- ").append(p.get("name"))
              .append(" (Gia: ").append(com.ares.shop.service.MailService.vnd((Long) p.get("price")))
              .append(", Trang thai: ").append((Boolean.TRUE.equals(p.get("inStock")) || (p.get("stock") != null && ((Integer) p.get("stock")) > 0)) ? "Con hang" : "Tam het")
              .append(")\n");
        }
        return sb.toString();
    }

    /**
     * Bo tri thuc chuyen gia ve mo hinh xe (Diecast & RC) cua Ares Toys.
     */
    private String buildExpertConsultation(String query, List<Map<String, Object>> products) {
        String n = norm(query);

        // 1. Tu van ve ty le
        if (n.contains("1:64") || n.contains("1 64") || n.contains("ty le 64")) {
            return "Ty le 1:64 la kich thuoc pho bien va duoc yeu thich nhat trong gioi suu tam (chieu dai khoang 7 - 8 cm). Uu diem vuot troi la nho gon, de trung bay ca bo suu tap tren ban lam viec hoac ke go nho. Tai ARES Toys, dong 1:64 gom Hot Wheels Premium (khung kim loai, banh cao su Real Riders), Mini GT (chi tiet sieu sac net) va Tomica. Ban co the tham khao cac mau hot ben duoi.";
        }
        if (n.contains("1:18") || n.contains("1 18") || n.contains("ty le 18")) {
            return "Ty le 1:18 la dinh cao cua mo hinh tinh cao cap (chieu dai khoang 25 - 28 cm). Dong nay mo duoc toan bo cua xe, nap capo thay ro khoi dong co V8/V12, chi tiet noi that boc da gia lap va vo-lang co the danh lai banh truoc. Thich hop dat trong tu kinh phong khach hoac lam qua tang sang trong cho doi tac.";
        }
        if (n.contains("1:24") || n.contains("1 24") || n.contains("ty le 24")) {
            return "Ty le 1:24 co chieu dai khoang 18 - 20 cm, la lua chon can bang hoan hao giua do chi tiet va khong gian trung bay. Mo duoc 2 cua va nap capo, gia thanh rat de tiep can (khoang 400.000d - 800.000d). Day la ty le duoc nhieu nguoi moi bat dau suu tam lua chon nhat.";
        }
        if (n.contains("ty le") || n.contains("scale") || n.contains("so sanh ty le")) {
            return "Tai ARES Toys, xe mo hinh co 4 ty le chu dao:\n"
                    + "- 1:64 (~7-8cm): Nho gon, de suu tam so luong lon (Hot Wheels, Mini GT, Tomica).\n"
                    + "- 1:43 (~10-12cm): Chuan suu tap chau Au, ti mi va gon gang.\n"
                    + "- 1:24 (~18-20cm): Mo duoc cua xe, gia de tiep can.\n"
                    + "- 1:18 (~25-28cm): Mo full chi tiet dong co va noi that, danh lai theo vo-lang, sang trong nhat.\n"
                    + "Ban muon chon mau xe de ban lam viec hay lam qua tang?";
        }

        // 2. Tu van ve xe dieu khien RC
        if (n.contains("rc") || n.contains("dieu khien") || n.contains("drift") || n.contains("crawler") || n.contains("buggy")) {
            return "Xe dieu khien tu xa (RC) tai ARES Toys su dung song 2.4GHz chong trung kenh, tam xa len toi 80 - 100m. Co 3 dong chinh:\n"
                    + "- RC Drift: Lop nhua cung giup truot xe dep mat tren san gach men/be tong phang.\n"
                    + "- RC Rock Crawler: He dan dong 4WD, cau mem vuot moi dia hinh gap ghenh, da soi.\n"
                    + "- RC Buggy / Truggy: Toc do cao len den 40 - 60km/h, he thong giam xoc dau chiu va dap tot.\n"
                    + "Tat ca deu co san linh kien thay the (pin, motor, servo) de ban thoa suc nang cap!";
        }

        // 3. Tu van qua tang / ngan sach
        if (n.contains("qua tang") || n.contains("tang ban") || n.contains("tang ban trai") || n.contains("tang chong") || n.contains("tang sinh nhat") || n.contains("ngan sach") || n.contains("duoi")) {
            StringBuilder sb = new StringBuilder();
            sb.append("ARES Toys goi y cac lua chon qua tang cuc ky y nghia theo ngan sach:\n");
            sb.append("- Duoi 500k: Hot Wheels Premium kim loai banh cao su, Mini GT Lamborghini Huracan hoac Tomica Premium.\n");
            sb.append("- Tu 500k - 1.5 trieu: Xe mo hinh 1:24 Bburago/Maisto mo duoc cua, hoac xe RC drift WLtoys day du phu kien.\n");
            sb.append("- Tren 1.5 trieu: Mo hinh 1:18 sang trong mo full chi tiet may, hoac xe RC Off-road Traxxas manh me.\n");
            sb.append("ARES Toys ho tro dong goi hop qua cao cap kem thiep chuc mung mien phi.");
            return sb.toString();
        }

        // 4. Chat lieu & bao quan
        if (n.contains("chat lieu") || n.contains("hop kim") || n.contains("kim loai") || n.contains("zamak") || n.contains("bao quan") || n.contains("ve sinh")) {
            return "Mo hinh tinh tai ARES Toys duoc duc tu hop kim kem Zamak cao cap, tao do dam tay va khong bi oxi hoa theo thoi gian, son tinh dien da lop bong nhu xe that.\n"
                    + "Meo bao quan xe luon nhu moi:\n"
                    + "1. Trung bay trong tu kinh hoac hop mica de tranh bui va am.\n"
                    + "2. Ve sinh bang choi long sieu mem hoac khan microfiber, khong dung con hay hoa chat tay rua.\n"
                    + "3. Tranh de duoi anh nang truc tiep chieu lau ngay de giu lop son luon ben mau.";
        }

        // 5. Mau xe cu the duoc hoi
        if (!products.isEmpty()) {
            Map<String, Object> p = products.get(0);
            return "Mau xe \"" + p.get("name") + "\" hien dang co mat tai ARES Toys voi gia " + com.ares.shop.service.MailService.vnd((Long) p.get("price"))
                    + ". Day la mau mo hinh co do hoan thien tinh xao, chat lieu hop kim duc chac chan, son bong dep mat va rat dang gia de bo sung vao bo suu tap. Ban co the xem chi tiet hoac them ngay vao gio hang ben duoi nhe!";
        }

        // 6. Mac dinh tu van chung
        return "ARES Toys xin chao ban! Toi la chuyen gia tu van cua shop. Shop chuyen cung cap xe mo hinh tinh diecast chinh hang (Hot Wheels, Mini GT, Tomica, Bburago...) va cac dong xe RC dieu khien toc do cao. Ban dang tim mau xe theo so thich, thuong hieu cu the, hay can goi y qua tang theo muc ngan sach nao?";
    }

    /**
     * Tim cac san pham phu hop tu CSDL de hien thi card tuong tac trong khung chat.
     */
    private List<Map<String, Object>> findRelevantProducts(String q) {
        String n = norm(q);
        List<String> words = Arrays.stream(n.split(" "))
                .filter(w -> w.length() > 1 && !STOP.contains(w))
                .toList();

        Long maxPrice = null;
        Matcher pm = Pattern.compile("(duoi|<)\\s*(\\d+)\\s*(k|tr|trieu)?").matcher(n);
        if (pm.find()) {
            long v = Long.parseLong(pm.group(2));
            String unit = pm.group(3);
            maxPrice = "k".equals(unit) ? v * 1000 : unit != null ? v * 1_000_000 : v < 1000 ? v * 1_000_000 : v;
        }
        final Long mp = maxPrice;

        return productRepo.findAll().stream()
                .filter(p -> p.active && (mp == null || p.price <= mp))
                .map(p -> {
                    String hay = norm(p.name + " " + (p.brand == null ? "" : p.brand.name) + " " + p.type + " " + p.scale + " " + p.grp);
                    long score = words.stream().filter(hay::contains).count();
                    if (words.isEmpty() && mp != null) score = 1;
                    return Map.entry(p, score);
                })
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> a.getValue().equals(b.getValue()) ? Integer.compare(b.getKey().sold, a.getKey().sold) : Long.compare(b.getValue(), a.getValue()))
                .limit(4)
                .map(e -> {
                    Product p = e.getKey();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", p.id);
                    m.put("name", p.name);
                    m.put("price", p.price);
                    m.put("image", p.images.isEmpty() ? "" : p.images.get(0));
                    m.put("stock", p.stock);
                    m.put("inStock", p.stock > 0);
                    m.put("scale", p.scale);
                    return m;
                })
                .collect(Collectors.toList());
    }

    private List<String> buildFollowUpQuick(String q) {
        String n = norm(q);
        if (n.contains("rc") || n.contains("dieu khien")) {
            return List.of("Xe RC Drift", "Xe RC Rock Crawler", "Pin va phu kien RC", "Chinh sach bao hanh");
        }
        if (n.contains("ty le") || n.contains("1:64") || n.contains("1:18")) {
            return List.of("Xe Hot Wheels 1:64", "Xe 1:18 mo full cua", "Tu trung bay mica", "Goi y qua tang");
        }
        if (n.contains("qua") || n.contains("tang") || n.contains("ngan sach")) {
            return List.of("Xe duoi 500k", "Xe 500k - 1.5 trieu", "Goi qua mien phi", "Ma giam gia");
        }
        return List.of("Tư vấn chọn quà", "So sánh tỷ lệ 1:64 và 1:18", "Tư vấn xe RC", "Cách bảo quản xe", "Gặp nhân viên");
    }

    private static final Set<String> STOP = Set.of(
            "cho", "toi", "minh", "ban", "shop", "co", "khong", "la", "gi", "nao", "the", "voi", "mot",
            "can", "muon", "tim", "mua", "xem", "ve", "cua", "va", "hay", "duoc", "nhe", "a", "oi", "em",
            "anh", "chi", "hoi", "o", "dau", "bao", "nhieu", "gia", "san", "pham", "loai"
    );
}
