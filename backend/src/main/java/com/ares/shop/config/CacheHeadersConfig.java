package com.ares.shop.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Header cache cho API (v16):
 * - Dữ liệu công khai ít đổi (brands, categories, sitemap, robots, SEO meta/render, bài viết): public, max-age ngắn + stale-while-revalidate.
 * - Danh mục sản phẩm & dữ liệu công khai khác: no-cache + ETag (trình duyệt luôn kiểm tra lại, nhận 304 nếu không đổi).
 * - Có Authorization hoặc /api/admin, /api/auth, /api/account…: private, no-store.
 */
@Configuration
public class CacheHeadersConfig {
    static final Pattern SHORT = Pattern.compile("^/(api/brands|api/categories|api/public/(seo/.*|posts(/tags)?|currency|manifest\\.webmanifest))$");
    static final Pattern PUBLIC = Pattern.compile("^/api/(products(/.*)?|public/.*|brands|categories|reviews/.*)$");
    static final Pattern PRIVATE = Pattern.compile("^/api/(admin|auth|account|orders|cart|wishlist|notifications|me|payments|chat)(/.*)?$");

    @Bean
    public FilterRegistrationBean<Filter> cacheHeaders() {
        Filter f = (req, res, chain) -> {
            HttpServletRequest rq = (HttpServletRequest) req; HttpServletResponse rs = (HttpServletResponse) res;
            String p = rq.getRequestURI(); boolean get = "GET".equals(rq.getMethod()) || "HEAD".equals(rq.getMethod());
            boolean auth = rq.getHeader("Authorization") != null;
            if (p.startsWith("/uploads/")) rs.setHeader("Cache-Control", "public, max-age=31536000, immutable"); // tên tệp ngẫu nhiên, không bao giờ ghi đè
            else if (!get || auth || PRIVATE.matcher(p).matches()) { if (p.startsWith("/api/")) rs.setHeader("Cache-Control", "private, no-store"); }
            else if (SHORT.matcher(p).matches()) rs.setHeader("Cache-Control", "public, max-age=300, stale-while-revalidate=600");
            else if (PUBLIC.matcher(p).matches()) rs.setHeader("Cache-Control", "no-cache");
            if (p.startsWith("/api/") || p.equals("/sitemap.xml") || p.equals("/robots.txt")) rs.setHeader("Vary", "Authorization, Accept-Encoding");
            chain.doFilter(req, res);
        };
        FilterRegistrationBean<Filter> b = new FilterRegistrationBean<>(f); b.addUrlPatterns("/*"); b.setOrder(Ordered.HIGHEST_PRECEDENCE + 10); return b;
    }

    /** ETag (băm nội dung) cho GET công khai không đăng nhập → 304 Not Modified. */
    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> etag() {
        ShallowEtagHeaderFilter e = new ShallowEtagHeaderFilter() {
            @Override protected boolean shouldNotFilter(HttpServletRequest r) {
                String p = r.getRequestURI();
                return r.getHeader("Authorization") != null || p.startsWith("/ws") || p.contains("/stream") || p.contains("/sse") || p.startsWith("/uploads/")
                    || !(PUBLIC.matcher(p).matches() || SHORT.matcher(p).matches());
            }
        };
        e.setWriteWeakETag(true); // ETag yếu: Tomcat vẫn gzip được (ETag mạnh sẽ chặn nén)
        FilterRegistrationBean<ShallowEtagHeaderFilter> b = new FilterRegistrationBean<>(e); b.addUrlPatterns("/*"); b.setOrder(Ordered.HIGHEST_PRECEDENCE + 20); return b;
    }
}
