package com.ares.shop.config;

import com.ares.shop.model.AuditLog;
import com.ares.shop.repo.AuditRepo;
import com.ares.shop.web.Current;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;

/** Ghi nhật ký mọi request thay đổi dữ liệu (POST/PUT/PATCH/DELETE) vào /api/admin/**. */
@Configuration
public class AuditConfig implements WebMvcConfigurer {
    private final AuditRepo repo;
    public AuditConfig(AuditRepo repo) { this.repo = repo; }

    @Override public void addInterceptors(InterceptorRegistry r) {
        r.addInterceptor(new HandlerInterceptor() {
            @Override public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object h, Exception ex) {
                if ("GET".equals(req.getMethod()) || "OPTIONS".equals(req.getMethod())) return;
                try {
                    var u = Current.userOrNull();
                    AuditLog a = new AuditLog();
                    a.actor = u == null ? "?" : u.email; a.role = u == null ? null : u.role.name();
                    a.method = req.getMethod(); a.path = req.getRequestURI(); a.status = res.getStatus();
                    a.summary = describe(a.method, a.path);
                    repo.save(a);
                } catch (Exception ignored) {}
            }
        }).addPathPatterns("/api/admin/**");
    }

    static String describe(String m, String p) {
        String s = p.replace("/api/admin/", "");
        String what = s.startsWith("orders/bulk") ? "Cập nhật hàng loạt đơn" : s.startsWith("orders") ? "Đơn hàng" : s.startsWith("products/bulk") ? "Sửa hàng loạt sản phẩm"
                : s.startsWith("products/import") ? "Nhập CSV sản phẩm" : s.startsWith("products") ? "Sản phẩm" : s.startsWith("inventory") ? "Kho" : s.startsWith("customers") ? "Khách hàng"
                : s.startsWith("promotions") ? "Voucher" : s.startsWith("flash") ? "Flash sale" : s.startsWith("banners") ? "Banner" : s.startsWith("returns") ? "Đổi trả"
                : s.startsWith("tickets") ? "Phiếu hỗ trợ" : s.startsWith("permissions") ? "Phân quyền" : s.startsWith("staff") ? "Nhân viên" : s;
        String verb = switch (m) { case "POST" -> "Tạo/Thao tác"; case "PUT" -> "Cập nhật"; case "PATCH" -> "Cập nhật"; case "DELETE" -> "Xoá"; default -> m; };
        return verb + " · " + what;
    }
}
