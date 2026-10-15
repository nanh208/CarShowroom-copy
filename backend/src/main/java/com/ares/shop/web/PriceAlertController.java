package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

/** Báo giảm giá: khách (đăng nhập hoặc chỉ email) + giá mục tiêu; lịch sử giá công khai; admin xem danh sách. */
@RestController
public class PriceAlertController {
    private final AlertRepo alerts; private final ProductRepo products; private final PriceHistoryRepo history;
    public PriceAlertController(AlertRepo alerts, ProductRepo products, PriceHistoryRepo history) { this.alerts = alerts; this.products = products; this.history = history; }

    public record Req(Long productId, String email, Long targetPrice) {}

    @PostMapping("/api/public/price-alerts")
    public Map<String, Object> subscribe(@RequestBody Req r) {
        Product p = products.findById(Optional.ofNullable(r.productId()).orElse(-1L)).filter(x -> x.active).orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm"));
        User u = Current.userOrNull();
        String email = r.email() == null ? null : r.email().trim().toLowerCase();
        if (u == null && (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) throw ApiException.bad("Vui lòng nhập email hợp lệ để nhận thông báo");
        if (r.targetPrice() != null && (r.targetPrice() <= 0 || r.targetPrice() >= p.price)) throw ApiException.bad("Giá mong muốn phải nhỏ hơn giá hiện tại");
        WatchAlert w = alerts.findAll().stream().filter(x -> "PRICE".equals(x.kind) && x.productId.equals(p.id) && !Boolean.TRUE.equals(x.fired)
                && (u != null ? x.user != null && x.user.id.equals(u.id) : email.equals(x.email))).findFirst().orElseGet(WatchAlert::new);
        w.user = u; w.email = u == null ? email : null; w.productId = p.id; w.kind = "PRICE"; w.basePrice = p.price; w.targetPrice = r.targetPrice(); w.fired = false;
        alerts.save(w);
        return Map.of("id", w.id, "basePrice", p.price, "targetPrice", r.targetPrice() == null ? 0 : r.targetPrice(),
                "message", r.targetPrice() == null ? "Sẽ báo bạn khi sản phẩm giảm giá" : "Sẽ báo bạn khi giá còn " + String.format("%,d", r.targetPrice()).replace(',', '.') + "đ hoặc thấp hơn");
    }

    @GetMapping("/api/products/{id}/price-history")
    public Map<String, Object> priceHistory(@PathVariable Long id) {
        Product p = products.findById(id).filter(x -> x.active).orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm"));
        var list = history.findByProductIdOrderByAtAsc(id);
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        long min30 = list.stream().filter(h -> h.at.isAfter(since)).mapToLong(h -> h.price).min().orElse(p.price);
        // giá có hiệu lực ngay trước mốc 30 ngày cũng tính
        for (var h : list) if (!h.at.isAfter(since)) min30 = Math.min(min30, h.price);
        min30 = Math.min(min30, p.price);
        long watchers = alerts.findAll().stream().filter(x -> "PRICE".equals(x.kind) && x.productId.equals(id) && !Boolean.TRUE.equals(x.fired)).count();
        return Map.of("points", list.stream().map(h -> Map.of("price", h.price, "at", h.at)).toList(), "current", p.price, "min30", min30,
                "lowest", p.price <= min30, "watchers", watchers);
    }

    @GetMapping("/api/admin/price-alerts")
    public List<Map<String, Object>> adminList() {
        Map<Long, Product> ps = new HashMap<>(); products.findAll().forEach(p -> ps.put(p.id, p));
        return alerts.findAll().stream().filter(x -> "PRICE".equals(x.kind)).sorted(Comparator.comparing((WatchAlert x) -> x.createdAt).reversed()).map(x -> {
            Map<String, Object> m = new LinkedHashMap<>(); Product p = ps.get(x.productId);
            m.put("id", x.id); m.put("productId", x.productId); m.put("productName", p == null ? "?" : p.name); m.put("currentPrice", p == null ? null : p.price);
            m.put("basePrice", x.basePrice); m.put("targetPrice", x.targetPrice); m.put("contact", x.user != null ? x.user.email : x.email);
            m.put("fired", Boolean.TRUE.equals(x.fired)); m.put("firedAt", x.firedAt); m.put("firedPrice", x.firedPrice); m.put("createdAt", x.createdAt); return m; }).toList();
    }
}
