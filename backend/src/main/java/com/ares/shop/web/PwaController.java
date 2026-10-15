package com.ares.shop.web;

import com.ares.shop.repo.SettingRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** Web App Manifest (PWA) do backend sinh: màu nền theo chế độ sáng/tối trong cài đặt giao diện, lối tắt tới trang hay dùng. */
@RestController
public class PwaController {
    private final SettingRepo settings; private final ObjectMapper json = new ObjectMapper();
    public PwaController(SettingRepo settings) { this.settings = settings; }

    @GetMapping(value = "/api/public/manifest.webmanifest", produces = "application/manifest+json")
    public Map<String, Object> manifest() {
        boolean dark = settings.findById("appearance").map(s -> { try { return "dark".equals(json.readValue(s.v, Map.class).get("mode")); } catch (Exception e) { return false; } }).orElse(false);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", "ARES Toys — Xe mô hình & RC"); m.put("short_name", "ARES Toys"); m.put("lang", "vi");
        m.put("description", "Xe mô hình die-cast, xe RC và phụ kiện chính hãng.");
        m.put("start_url", "/?source=pwa"); m.put("scope", "/"); m.put("display", "standalone"); m.put("orientation", "portrait");
        m.put("background_color", dark ? "#121212" : "#f4efe6"); m.put("theme_color", "#f26522");
        m.put("icons", List.of(Map.of("src", "/icons/icon-192.png", "sizes", "192x192", "type", "image/png"), Map.of("src", "/icons/icon-512.png", "sizes", "512x512", "type", "image/png"),
            Map.of("src", "/icons/maskable-512.png", "sizes", "512x512", "type", "image/png", "purpose", "maskable")));
        m.put("shortcuts", List.of(Map.of("name", "Sản phẩm", "url", "/products"), Map.of("name", "Giỏ hàng", "url", "/cart"), Map.of("name", "Tra cứu đơn", "url", "/track"), Map.of("name", "Đấu giá", "url", "/auctions")));
        m.put("categories", List.of("shopping"));
        return m;
    }
}
