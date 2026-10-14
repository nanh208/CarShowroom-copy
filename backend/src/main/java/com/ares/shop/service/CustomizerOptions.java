package com.ares.shop.service;

import java.util.*;

/** Cấu hình xe: màu sơn, mâm, nội thất (giá cộng thêm, VND). */
public final class CustomizerOptions {
    private CustomizerOptions() {}
    public record Opt(String id, String name, String hex, long price) {}

    public static final List<Opt> PAINTS = List.of(
        new Opt("original", "Màu nguyên bản", null, 0),
        new Opt("rosso", "Đỏ Rosso Corsa", "#c1121f", 45_000_000),
        new Opt("blu", "Xanh Blu Nart", "#1d4ed8", 45_000_000),
        new Opt("nero", "Đen bóng Nero", "#111827", 30_000_000),
        new Opt("bianco", "Trắng ngọc trai", "#f8fafc", 60_000_000),
        new Opt("giallo", "Vàng Giallo", "#facc15", 45_000_000),
        new Opt("verde", "Xanh lá Verde Mantis", "#65a30d", 80_000_000),
        new Opt("carbon", "Carbon mờ", "#3f3f46", 150_000_000));
    public static final List<Opt> WHEELS = List.of(
        new Opt("standard", "Mâm tiêu chuẩn 19\"", "#cbd5e1", 0),
        new Opt("black", "Mâm đen thể thao 20\"", "#1f2937", 25_000_000),
        new Opt("gold", "Mâm rèn vàng đồng 21\"", "#b8860b", 30_000_000),
        new Opt("chrome", "Mâm chrome 21\"", "#e5e7eb", 45_000_000));
    public static final List<Opt> INTERIORS = List.of(
        new Opt("black", "Da đen", "#0f172a", 0),
        new Opt("tan", "Da bò Tan", "#a16207", 35_000_000),
        new Opt("red", "Da đỏ", "#991b1b", 35_000_000),
        new Opt("white", "Da trắng kem", "#f5f5dc", 50_000_000));

    public static Map<String, List<Opt>> all() {
        return Map.of("paints", PAINTS, "wheels", WHEELS, "interiors", INTERIORS);
    }

    public static Opt find(List<Opt> list, String id) {
        if (id == null || id.isBlank()) return list.get(0);
        return list.stream().filter(o -> o.id().equals(id)).findFirst().orElse(list.get(0));
    }
}
