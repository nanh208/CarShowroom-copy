package com.ares.shop.service;

import com.ares.shop.repo.SettingRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Đa ngôn ngữ vi/en. Khoá = chuỗi tiếng Việt gốc (kiểu gettext); bản dịch mặc định trong resources/i18n/{lang}.json,
 * admin có thể ghi đè (AppSetting "i18n.{lang}"). Mục "_errors" dịch thông báo lỗi API theo Accept-Language.
 */
@Service
public class I18nService {
    public static final List<String> LANGS = List.of("vi", "en");
    private final SettingRepo settings; private final ObjectMapper json = new ObjectMapper();
    private final Map<String, Map<String, Object>> base = new HashMap<>();
    public I18nService(SettingRepo settings) {
        this.settings = settings;
        for (String l : LANGS) try { var r = new ClassPathResource("i18n/" + l + ".json"); base.put(l, r.exists() ? json.readValue(r.getInputStream(), Map.class) : new HashMap<>()); }
            catch (Exception e) { base.put(l, new HashMap<>()); }
    }
    @SuppressWarnings("unchecked")
    public Map<String, String> overrides(String lang) {
        return settings.findById("i18n." + lang).map(s -> { try { return (Map<String, String>) json.readValue(s.v, Map.class); } catch (Exception e) { return new HashMap<String, String>(); } }).orElseGet(HashMap::new);
    }
    /** Từ điển giao diện đã gộp (mặc định + ghi đè của admin). */
    public Map<String, String> dict(String lang) {
        Map<String, String> m = new TreeMap<>();
        base.getOrDefault(lang, Map.of()).forEach((k, v) -> { if (!k.startsWith("_") && v instanceof String s) m.put(k, s); });
        m.putAll(overrides(lang));
        return m;
    }
    /** Tất cả khoá đã biết (từ bản tiếng Anh mặc định). */
    public Set<String> keys() { Set<String> k = new TreeSet<>(); base.getOrDefault("en", Map.of()).keySet().stream().filter(x -> !x.startsWith("_")).forEach(k::add); return k; }
    public String error(String msg, String lang) {
        if (msg == null || !"en".equals(lang)) return msg;
        Object e = base.getOrDefault("en", Map.of()).get("_errors");
        if (e instanceof Map<?, ?> em && em.get(msg) instanceof String s) return s;
        return msg;
    }
    public static String langOf(String acceptLanguage) { return acceptLanguage != null && acceptLanguage.toLowerCase().startsWith("en") ? "en" : "vi"; }
}
