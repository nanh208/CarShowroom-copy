package com.ares.shop.service;

import com.ares.shop.model.Product;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

/**
 * Mô phỏng API hãng vận chuyển kiểu GHN / GHTK (sandbox): báo giá theo vùng + khối lượng + COD,
 * tạo vận đơn (mã tracking), và sinh hành trình theo thời gian thực tế đã trôi qua.
 * Không gọi mạng; công thức gần với biểu phí công khai để số liệu hợp lý.
 */
@Service
public class CarrierService {
    public static final List<String> CARRIERS = List.of("GHN", "GHTK");
    public static final Map<String, String> NAMES = Map.of("GHN", "Giao Hàng Nhanh", "GHTK", "Giao Hàng Tiết Kiệm");

    public static int zone(String city) {
        if (city == null || city.isBlank()) return 2;
        if (OrderService.INNER.contains(city)) return 0;
        if (OrderService.NEAR.contains(city)) return 1;
        return 2;
    }
    /** Khối lượng ước tính (gram) theo tỉ lệ mô hình — sản phẩm chưa có trường cân nặng. */
    public static int weightOf(Product p) {
        if (p == null) return 300;
        if ("ACCESSORY".equals(p.grp)) return 300;
        String s = p.scale == null ? "" : p.scale;
        return switch (s) { case "1:64" -> 120; case "1:43" -> 300; case "1:32" -> 450; case "1:24" -> 700; case "1:18" -> 1600; case "1:12", "1:10", "1:8" -> 2600; default -> 800; };
    }

    public record Quote(String carrier, String service, String label, long fee, long codFee, int weight, LocalDate etaFrom, LocalDate etaTo) {}

    /** service: STANDARD | EXPRESS. */
    public Quote quote(String carrier, String service, String city, int grams, long codAmount) {
        carrier = carrier == null ? "GHN" : carrier.toUpperCase(); boolean ex = "EXPRESS".equalsIgnoreCase(service);
        int z = zone(city); int w = Math.max(grams, 100);
        long base, step; int lo;
        if ("GHTK".equals(carrier)) { base = new long[]{16_500, 25_000, 32_000}[z]; step = new long[]{2_500, 5_000, 5_000}[z]; lo = new int[]{1, 2, 3}[z]; }
        else { base = new long[]{18_500, 27_000, 34_000}[z]; step = new long[]{2_500, 4_000, 5_000}[z]; lo = new int[]{1, 2, 2}[z]; }
        long extra = w > 500 ? (long) Math.ceil((w - 500) / 500.0) * step : 0;
        long fee = base + extra;
        if (ex) { fee = Math.round(fee * 1.6 / 500) * 500 + 10_000; lo = Math.max(1, lo - 1); }
        long cod = codAmount > 0 ? Math.max(0, Math.round(codAmount * ("GHTK".equals(carrier) ? 0.005 : 0.004) / 500) * 500) : 0;
        if (cod > 0 && codAmount <= 3_000_000 && "GHTK".equals(carrier)) cod = 0; // GHTK miễn phí COD ≤ 3 triệu
        var from = LocalDate.now().plusDays(lo);
        String label = NAMES.getOrDefault(carrier, carrier) + (ex ? " · Hoả tốc" : " · Tiêu chuẩn");
        return new Quote(carrier, ex ? "EXPRESS" : "STANDARD", label, Math.round(fee / 500.0) * 500, cod, w, from, from.plusDays(ex ? 1 : 2));
    }

    public List<Quote> quoteAll(String city, int grams, long cod) {
        List<Quote> l = new ArrayList<>();
        for (String c : CARRIERS) for (String s : List.of("STANDARD", "EXPRESS")) l.add(quote(c, s, city, grams, cod));
        l.sort(Comparator.comparingLong(Quote::fee));
        return l;
    }

    public String newTracking(String carrier) {
        String r = Long.toString(Math.abs(new Random().nextLong()), 36).toUpperCase();
        return ("GHTK".equals(carrier) ? "S" + r.substring(0, 9) : "GHN" + r.substring(0, 8));
    }

    /** Hành trình mô phỏng dựa trên thời gian từ lúc bàn giao: lấy hàng → kho phân loại → đang giao → (giao thành công khi tới ETA). */
    public List<Map<String, Object>> events(String carrier, String trackingNo, LocalDateTime handedOver, String city, boolean delivered) {
        List<Map<String, Object>> ev = new ArrayList<>();
        if (handedOver == null) return ev;
        String hub = "GHTK".equals(carrier) ? "Kho GHTK" : "Bưu cục GHN";
        Object[][] steps = {{0, "PICKED_UP", "Đã lấy hàng tại ARES Toys"}, {2, "AT_HUB", hub + " phân loại (TP. Hồ Chí Minh)"},
            {12, "IN_TRANSIT", "Đang trung chuyển tới " + (city == null ? "điểm đến" : city)}, {30, "OUT_FOR_DELIVERY", "Shipper đang giao hàng"}};
        long hours = Duration.between(handedOver, LocalDateTime.now()).toHours();
        for (Object[] s : steps) if (hours >= (int) s[0] || delivered) ev.add(Map.of("at", handedOver.plusHours((int) s[0]), "code", s[1], "text", s[2]));
        if (delivered) ev.add(Map.of("at", LocalDateTime.now(), "code", "DELIVERED", "text", "Giao hàng thành công"));
        return ev;
    }
}
