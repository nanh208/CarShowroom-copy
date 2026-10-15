package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.SettingRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** Giao diện toàn site do admin cấu hình (áp dụng cho mọi khách): theme 2.0/1.0, sáng/tối, cỡ chữ, tương phản, mật độ, ảnh nền. */
@RestController
public class AppearanceController {
    static final Map<String, Object> DEFAULTS;
    static {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("theme", "v2"); d.put("mode", "light"); d.put("fontScale", 1.0); d.put("contrast", false); d.put("density", "comfy");
        d.put("bgImage", ""); d.put("bgOverlay", 0.78); d.put("bgBlur", 0); d.put("bgFit", "cover"); d.put("bgAttach", "fixed"); d.put("bgPages", "all");
        DEFAULTS = Collections.unmodifiableMap(d);
    }
    private final SettingRepo settings; private final ObjectMapper json = new ObjectMapper();
    public AppearanceController(SettingRepo settings) { this.settings = settings; }

    @GetMapping("/api/public/appearance")
    public Map<String, Object> get() {
        Map<String, Object> m = new LinkedHashMap<>(DEFAULTS);
        settings.findById("appearance").ifPresent(s -> { try { m.putAll(json.readValue(s.v, Map.class)); } catch (Exception ignored) {} });
        m.keySet().retainAll(DEFAULTS.keySet()); return m;
    }

    @PutMapping("/api/admin/appearance")
    public Map<String, Object> save(@RequestBody Map<String, Object> b) throws Exception {
        if (Current.user().role != Enums.Role.ADMIN) throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "Chỉ quản trị viên được đổi giao diện");
        Map<String, Object> m = get();
        pick(b, m, "theme", Set.of("v2", "v1")); pick(b, m, "mode", Set.of("light", "dark")); pick(b, m, "density", Set.of("comfy", "compact"));
        pick(b, m, "bgFit", Set.of("cover", "contain", "repeat")); pick(b, m, "bgAttach", Set.of("fixed", "scroll")); pick(b, m, "bgPages", Set.of("all", "home"));
        if (b.get("fontScale") instanceof Number n) { double v = n.doubleValue(); if (!List.of(0.9, 1.0, 1.125, 1.25).contains(v)) throw ApiException.bad("Cỡ chữ không hợp lệ"); m.put("fontScale", v); }
        if (b.containsKey("contrast")) m.put("contrast", Boolean.TRUE.equals(b.get("contrast")));
        if (b.get("bgOverlay") instanceof Number n) m.put("bgOverlay", Math.max(0.3, Math.min(0.95, n.doubleValue()))); // tối thiểu 30% để chữ luôn đọc được
        if (b.get("bgBlur") instanceof Number n) m.put("bgBlur", Math.max(0, Math.min(20, n.intValue())));
        if (b.containsKey("bgImage")) {
            String u = Optional.ofNullable(b.get("bgImage")).map(Object::toString).orElse("").trim();
            if (!u.isEmpty() && !(u.startsWith("/uploads/") || u.startsWith("/images/") || u.matches("^https://[^\\s\"'()<>]+$"))) throw ApiException.bad("Ảnh nền phải là ảnh đã tải lên hoặc URL https");
            if (u.length() > 500) throw ApiException.bad("URL quá dài");
            m.put("bgImage", u);
        }
        AppSetting s = new AppSetting(); s.k = "appearance"; s.v = json.writeValueAsString(m); settings.save(s); return m;
    }
    @DeleteMapping("/api/admin/appearance")
    public Map<String, Object> reset() {
        if (Current.user().role != Enums.Role.ADMIN) throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "Chỉ quản trị viên được đổi giao diện");
        settings.deleteById("appearance"); return get();
    }
    private static void pick(Map<String, Object> b, Map<String, Object> m, String k, Set<String> ok) {
        if (!b.containsKey(k)) return; Object v = b.get(k); if (v == null || !ok.contains(v.toString())) throw ApiException.bad("Giá trị không hợp lệ: " + k); m.put(k, v.toString());
    }
}
