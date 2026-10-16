package com.ares.shop.service;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

/**
 * Dự báo nhu cầu tồn kho từ lịch sử bán: chuỗi bán theo ngày (đơn không huỷ/hoàn) → làm trơn mũ Holt có giảm chấn xu hướng (φ=0.9),
 * độ lệch chuẩn sai số → tồn kho an toàn = z·σ·√(lead). Gợi ý đặt hàng = nhu cầu (lead + horizon) + an toàn − tồn.
 */
@Service
public class ForecastService {
    final OrderRepo orders; final ProductRepo products;
    public ForecastService(OrderRepo orders, ProductRepo products) { this.orders = orders; this.products = products; }

    public Map<Long, double[]> dailySales(int days) {
        LocalDate start = LocalDate.now().minusDays(days - 1L); Map<Long, double[]> m = new HashMap<>();
        for (CustomerOrder o : orders.findAll()) {
            if (o.createdAt == null || o.status == Enums.OrderStatus.CANCELLED || o.status == Enums.OrderStatus.RETURNED) continue;
            LocalDate d = o.createdAt.toLocalDate(); if (d.isBefore(start)) continue;
            int idx = (int) (d.toEpochDay() - start.toEpochDay()); if (idx < 0 || idx >= days) continue;
            for (OrderItem i : o.items) if (i.productId != null) m.computeIfAbsent(i.productId, k -> new double[days])[idx] += i.quantity;
        }
        return m;
    }

    /** Holt linear: trả về {level, trend, sigma}. */
    static double[] holt(double[] y, double a, double b) {
        if (y.length == 0) return new double[]{0, 0, 0};
        double l = Arrays.stream(y, 0, Math.min(7, y.length)).average().orElse(0), t = 0, se = 0; int n = 0;
        for (double v : y) { double f = l + t; se += (v - f) * (v - f); n++; double nl = a * v + (1 - a) * (l + t); t = b * (nl - l) + (1 - b) * t; l = nl; }
        return new double[]{l, t, Math.sqrt(se / Math.max(1, n))};
    }

    public Map<String, Object> forecast(Product p, double[] y, int horizon, int lead, double z) {
        double[] h = holt(y, 0.3, 0.1); double level = Math.max(0, h[0]), trend = h[1], sigma = h[2];
        double demandH = 0, demandLead = 0;
        double damp = 0;
        for (int k = 1; k <= horizon + lead; k++) { damp += Math.pow(0.9, k); double f = Math.max(0, level + trend * damp); if (k <= lead) demandLead += f; demandH += f; }
        double daily = demandH / (horizon + lead), safety = z * sigma * Math.sqrt(lead);
        int reorderPoint = (int) Math.ceil(demandLead + safety), reorder = (int) Math.max(0, Math.ceil(demandH + safety - p.stock));
        Double cover = daily > 0.001 ? p.stock / daily : null;
        String risk = p.stock <= 0 && daily > 0 ? "OUT" : cover == null ? "NONE" : cover < lead ? "HIGH" : cover < lead + horizon / 2.0 ? "MEDIUM" : "LOW";
        double sold = Arrays.stream(y).sum(); int weeks = Math.max(1, y.length / 7); double[] wk = new double[Math.min(12, weeks)];
        for (int i = 0; i < wk.length; i++) { int end = y.length - i * 7; for (int j = Math.max(0, end - 7); j < end; j++) wk[wk.length - 1 - i] += y[j]; }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("productId", p.id); m.put("name", p.name); m.put("sku", p.sku); m.put("image", p.images.isEmpty() ? null : p.images.get(0)); m.put("stock", p.stock);
        m.put("sold", (int) sold); m.put("avgDaily", r2(daily)); m.put("trendPerDay", r2(trend)); m.put("forecast", (int) Math.round(demandH - demandLead)); m.put("leadDemand", (int) Math.round(demandLead));
        m.put("safetyStock", (int) Math.ceil(safety)); m.put("reorderPoint", reorderPoint); m.put("reorderQty", reorder); m.put("daysOfCover", cover == null ? null : r2(cover));
        m.put("stockoutDate", cover == null ? null : LocalDate.now().plusDays((long) Math.floor(cover))); m.put("risk", risk); m.put("weekly", Arrays.stream(wk).mapToInt(v -> (int) v).toArray());
        return m;
    }
    static double r2(double v) { return Math.round(v * 100) / 100.0; }

    public Map<String, Object> run(int days, int horizon, int lead, double serviceLevel) {
        days = Math.max(14, Math.min(days, 365)); horizon = Math.max(7, Math.min(horizon, 120)); lead = Math.max(1, Math.min(lead, 60));
        double z = serviceLevel >= 0.99 ? 2.33 : serviceLevel >= 0.975 ? 1.96 : serviceLevel >= 0.95 ? 1.65 : serviceLevel >= 0.9 ? 1.28 : 0.84;
        var sales = dailySales(days); List<Map<String, Object>> rows = new ArrayList<>(); final int d = days, hz = horizon, ld = lead;
        for (Product p : products.findAll()) { if (!p.active) continue; rows.add(forecast(p, sales.getOrDefault(p.id, new double[d]), hz, ld, z)); }
        Map<String, Integer> rank = Map.of("OUT", 0, "HIGH", 1, "MEDIUM", 2, "LOW", 3, "NONE", 4);
        rows.sort(Comparator.<Map<String, Object>>comparingInt(r -> rank.get((String) r.get("risk"))).thenComparing(r -> -((Number) r.get("reorderQty")).intValue()));
        Map<String, Long> counts = new TreeMap<>(); rows.forEach(r -> counts.merge((String) r.get("risk"), 1L, Long::sum));
        return Map.of("days", days, "horizon", horizon, "leadTime", lead, "serviceLevel", serviceLevel, "z", z, "counts", counts, "items", rows,
            "method", "Holt damped (α=0.3, β=0.1, φ=0.9) trên chuỗi bán theo ngày; tồn an toàn = z·σ·√lead");
    }
}
