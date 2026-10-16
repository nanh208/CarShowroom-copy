package com.ares.shop.config;

import com.ares.shop.model.Enums;
import com.ares.shop.repo.SettingRepo;
import com.ares.shop.web.ApiException;
import com.ares.shop.web.Current;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.*;

/**
 * Thực thi ma trận phân quyền nhân viên ở backend: STAFF chỉ gọi được /api/admin/{nhóm} khi nhóm đó được bật.
 * ADMIN không bị giới hạn. Endpoint chung (stats, kpi, uploads, permissions GET) không gắn nhóm nên luôn cho phép.
 */
@Component
public class StaffPermissionInterceptor implements HandlerInterceptor {
    static final Map<String, String> SECTION = new HashMap<>();
    static {
        String[][] m = {{"products", "products"}, {"orders", "orders"}, {"customers", "customers"}, {"customer-tags", "customers"}, {"inventory", "inventory"},
            {"warehouses", "inventory"}, {"returns", "returns"}, {"promotions", "promotions"}, {"flash-sales", "flash-sales"}, {"banners", "banners"},
            {"categories", "catalog"}, {"brands", "catalog"}, {"auctions", "auctions"}, {"questions", "questions"}, {"reviews", "reviews"}, {"tickets", "tickets"},
            {"chat", "chat"}, {"purchase-orders", "purchasing"}, {"restock-suggestions", "purchasing"}, {"abandoned-carts", "carts"}, {"segments", "segments"},
            {"bundles", "bundles"}, {"giftcards", "bundles"}, {"home-layout", "home-layout"}, {"emails", "emails"}, {"analytics", "analytics"},
            {"bot", "chatbot"}, {"preorders", "preorders"}, {"collections", "collections"}, {"notifications", "broadcast"}, {"price-alerts", "promotions"}, {"fulfillment", "orders"}, {"payments", "orders"}, {"import", "products"}, {"export", "products"}, {"i18n", "home-layout"}, {"subscriptions", "orders"}, {"affiliates", "promotions"}, {"affiliate-commissions", "promotions"}, {"currency", "home-layout"}, {"push", "broadcast"}, {"posts", "banners"}, {"image-search", "products"}};
        for (String[] r : m) SECTION.put(r[0], r[1]);
    }
    private final SettingRepo settings; private final ObjectMapper json = new ObjectMapper();
    public StaffPermissionInterceptor(SettingRepo settings) { this.settings = settings; }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object h) throws Exception {
        var u = Current.userOrNull();
        if (u == null || u.role != Enums.Role.STAFF) return true;
        String[] parts = req.getRequestURI().split("/"); // "", api, admin, seg
        if (parts.length < 4) return true;
        String sec = SECTION.get(parts[3]);
        if (sec == null) return true;
        var s = settings.findById("staffPerms").orElse(null);
        if (s == null) return true;
        List<?> allowed = json.readValue(s.v, List.class);
        if (!allowed.contains(sec)) throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản nhân viên chưa được cấp quyền mục này");
        return true;
    }
}
