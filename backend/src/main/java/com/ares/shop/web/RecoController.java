package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.*;

/**
 * Gợi ý sản phẩm từ lịch sử mua (và xem):
 * - "Mua cùng nhau": đồng xuất hiện trong cùng đơn (item-to-item collaborative filtering).
 * - "Dành cho bạn": điểm = đồng mua với các món đã mua + sở thích thương hiệu/tỉ lệ/loại từ đơn & lượt xem; loại món đã mua.
 * Khách chưa đăng nhập / chưa có lịch sử → bán chạy.
 */
@RestController
public class RecoController {
    private final OrderRepo orders; private final ProductRepo products; private final ProductViewRepo views;
    public RecoController(OrderRepo orders, ProductRepo products, ProductViewRepo views) { this.orders = orders; this.products = products; this.views = views; }

    static boolean counts(CustomerOrder o) { return o.status != Enums.OrderStatus.CANCELLED; }

    /** Ma trận đồng mua: pid → (pid khác → số đơn chung). */
    private Map<Long, Map<Long, Integer>> coMatrix() {
        Map<Long, Map<Long, Integer>> co = new HashMap<>();
        for (CustomerOrder o : orders.findAll()) {
            if (!counts(o)) continue;
            Set<Long> ids = o.items.stream().map(i -> i.productId).filter(Objects::nonNull).collect(Collectors.toSet());
            for (Long a : ids) for (Long b : ids) if (!a.equals(b)) co.computeIfAbsent(a, k -> new HashMap<>()).merge(b, 1, Integer::sum);
        }
        return co;
    }
    private static Map<String, Object> card(Product p, String reason) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("product", p); m.put("reason", reason); return m;
    }

    @GetMapping("/api/products/{id}/also-bought")
    public List<Product> alsoBought(@PathVariable Long id) {
        var row = coMatrix().getOrDefault(id, Map.of());
        List<Product> out = row.entrySet().stream().sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .map(e -> products.findById(e.getKey()).orElse(null)).filter(p -> p != null && p.active).limit(4).collect(Collectors.toCollection(ArrayList::new));
        if (out.size() < 4) { // bổ sung: cùng tỉ lệ / loại, bán chạy
            Product me = products.findById(id).orElse(null);
            if (me != null) products.findAll().stream().filter(p -> p.active && !p.id.equals(id) && out.stream().noneMatch(x -> x.id.equals(p.id)))
                .filter(p -> Objects.equals(p.scale, me.scale) || Objects.equals(p.type, me.type)).sorted(Comparator.comparingInt((Product p) -> -p.sold))
                .limit(4 - out.size()).forEach(out::add);
        }
        return out;
    }

    @GetMapping("/api/public/recommendations")
    public Map<String, Object> forMe(@RequestParam(defaultValue = "8") int limit) {
        limit = Math.max(1, Math.min(limit, 24));
        User u = Current.userOrNull();
        List<Product> all = products.findAll().stream().filter(p -> p.active).toList();
        if (u == null) return Map.of("personal", false, "items", all.stream().sorted(Comparator.comparingInt((Product p) -> -p.sold)).limit(limit).map(p -> card(p, "Bán chạy")).toList());
        var mine = orders.findByUserIdOrderByCreatedAtDesc(u.id).stream().filter(RecoController::counts).toList();
        Set<Long> bought = mine.stream().flatMap(o -> o.items.stream()).map(i -> i.productId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> viewed = views.findByUserIdOrderByViewedAtDesc(u.id).stream().map(v -> v.productId).limit(20).collect(Collectors.toSet());
        if (bought.isEmpty() && viewed.isEmpty())
            return Map.of("personal", false, "items", all.stream().sorted(Comparator.comparingInt((Product p) -> -p.sold)).limit(limit).map(p -> card(p, "Bán chạy")).toList());
        Map<Long, Product> byId = all.stream().collect(Collectors.toMap(p -> p.id, p -> p));
        Map<String, Integer> brandW = new HashMap<>(), scaleW = new HashMap<>(), typeW = new HashMap<>();
        for (Long id : bought) { Product p = byId.get(id); if (p == null) continue; if (p.brand != null) brandW.merge(p.brand.name, 3, Integer::sum); scaleW.merge(String.valueOf(p.scale), 3, Integer::sum); typeW.merge(String.valueOf(p.type), 2, Integer::sum); }
        for (Long id : viewed) { Product p = byId.get(id); if (p == null) continue; if (p.brand != null) brandW.merge(p.brand.name, 1, Integer::sum); scaleW.merge(String.valueOf(p.scale), 1, Integer::sum); typeW.merge(String.valueOf(p.type), 1, Integer::sum); }
        var co = coMatrix();
        Map<Long, Double> score = new HashMap<>(); Map<Long, String> why = new HashMap<>();
        for (Product p : all) {
            if (bought.contains(p.id)) continue;
            double s = 0; String reason = null; int coHits = 0; Long coFrom = null;
            for (Long b : bought) { int c = co.getOrDefault(b, Map.of()).getOrDefault(p.id, 0); if (c > coHits) { coHits = c; coFrom = b; } s += c * 5; }
            if (coHits > 0 && byId.containsKey(coFrom)) reason = "Thường mua cùng " + byId.get(coFrom).name;
            int bw = p.brand == null ? 0 : brandW.getOrDefault(p.brand.name, 0), sw = scaleW.getOrDefault(String.valueOf(p.scale), 0), tw = typeW.getOrDefault(String.valueOf(p.type), 0);
            s += bw * 1.5 + sw * 1.2 + tw;
            if (reason == null && bw > 0 && p.brand != null) reason = "Vì bạn thích " + p.brand.name;
            if (reason == null && sw > 0) reason = "Hợp bộ sưu tập tỉ lệ " + p.scale;
            if (viewed.contains(p.id)) { s += 2; if (reason == null) reason = "Bạn đã xem gần đây"; }
            s += Math.log1p(p.sold) * .3 + p.rating * .2;
            if (p.stock <= 0 && !Boolean.TRUE.equals(p.preorder)) s *= .4;
            if (reason == null) continue;
            score.put(p.id, s); why.put(p.id, reason);
        }
        var items = score.entrySet().stream().sorted(Map.Entry.<Long, Double>comparingByValue().reversed()).limit(limit)
            .map(e -> card(byId.get(e.getKey()), why.get(e.getKey()))).collect(Collectors.toCollection(ArrayList::new));
        if (items.size() < limit) all.stream().filter(p -> !bought.contains(p.id) && !score.containsKey(p.id)).sorted(Comparator.comparingInt((Product p) -> -p.sold))
            .limit(limit - items.size()).forEach(p -> items.add(card(p, "Bán chạy")));
        return Map.of("personal", true, "items", items);
    }
}
