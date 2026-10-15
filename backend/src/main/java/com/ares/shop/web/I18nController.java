package com.ares.shop.web;

import com.ares.shop.model.AppSetting;
import com.ares.shop.repo.SettingRepo;
import com.ares.shop.service.I18nService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class I18nController {
    private final I18nService i18n; private final SettingRepo settings; private final ObjectMapper json = new ObjectMapper();
    public I18nController(I18nService i18n, SettingRepo settings) { this.i18n = i18n; this.settings = settings; }

    @GetMapping("/api/public/i18n/{lang}")
    public Map<String, Object> dict(@PathVariable String lang) {
        if (!I18nService.LANGS.contains(lang)) throw ApiException.notFound("Ngôn ngữ không hỗ trợ");
        return Map.of("lang", lang, "messages", "vi".equals(lang) ? Map.of() : i18n.dict(lang), "languages", List.of(Map.of("code", "vi", "name", "Tiếng Việt"), Map.of("code", "en", "name", "English")));
    }

    /** Admin: bảng dịch (khoá gốc tiếng Việt → bản dịch hiện hành, có đánh dấu ghi đè). */
    @GetMapping("/api/admin/i18n")
    public List<Map<String, Object>> table() {
        var d = i18n.dict("en"); var ov = i18n.overrides("en");
        List<Map<String, Object>> out = new ArrayList<>();
        for (String k : i18n.keys()) out.add(Map.of("key", k, "en", d.getOrDefault(k, ""), "overridden", ov.containsKey(k)));
        return out;
    }
    @PutMapping("/api/admin/i18n/{lang}")
    public Map<String, Object> override(@PathVariable String lang, @RequestBody Map<String, String> body) throws Exception {
        if (!"en".equals(lang)) throw ApiException.bad("Chỉ ghi đè bản dịch tiếng Anh");
        var keys = i18n.keys(); var ov = new HashMap<>(i18n.overrides(lang));
        for (var e : body.entrySet()) {
            if (!keys.contains(e.getKey())) throw ApiException.bad("Khoá không tồn tại: " + e.getKey());
            String v = e.getValue() == null ? "" : e.getValue().trim();
            if (v.length() > 500) throw ApiException.bad("Bản dịch quá dài");
            if (v.isEmpty()) ov.remove(e.getKey()); else ov.put(e.getKey(), v);
        }
        AppSetting s = settings.findById("i18n." + lang).orElseGet(() -> { AppSetting n = new AppSetting(); n.k = "i18n." + lang; return n; });
        s.v = json.writeValueAsString(ov); if (s.v.length() > 8000) throw ApiException.bad("Tổng bản dịch ghi đè quá lớn"); settings.save(s);
        return Map.of("overrides", ov.size());
    }
}
