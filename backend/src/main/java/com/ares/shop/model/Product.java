package com.ares.shop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String name;
    @Column(unique = true) public String sku;
    @ManyToOne(fetch = FetchType.EAGER) public Brand brand;
    /** Mã danh mục (Category.code), VD: MODEL, RC, BATTERY. */
    public String type;
    /** TOY | ACCESSORY — suy ra từ danh mục. */
    public String grp = "TOY";
    public String scale;            // 1:64, 1:18, 1:10...
    public int ageMin = 8;          // độ tuổi khuyến nghị
    public String material;
    public int warrantyMonths;
    public long price;
    public Long oldPrice;
    public int stock;
    public int sold;
    public double rating;
    public int reviewCount;
    public String baseColor = "#a87a3c";
    @Column(length = 3000) public String description;
    /** URL video / placeholder 360° (tuỳ chọn). */
    public String videoUrl; public String view360;
    /** Mẫu sắp ra mắt: cho phép đặt trước, ngày phát hành dự kiến, tiền cọc. */
    public Boolean preorder; public java.time.LocalDate releaseDate; public Long preorderDeposit;
    @ElementCollection(fetch = FetchType.EAGER) @OrderColumn
    public List<String> images = new ArrayList<>();
    @ElementCollection(fetch = FetchType.EAGER) @MapKeyColumn(name = "spec_key") @Column(name = "spec_value")
    public Map<String, String> specs = new LinkedHashMap<>();
    public boolean hasVariants;
    public boolean isNew;
    public boolean featured;
    public boolean active = true;
    /** Đánh dấu sản phẩm dễ cháy nổ / pin LiPo (Lithium Polymer) / hoá chất. */
    public Boolean hazardous;
    /** Phụ phí an toàn vận chuyển & đóng gói chống cháy nổ (VNĐ, mặc định 25.000đ). */
    public Long hazardousFee;
    public LocalDateTime createdAt = LocalDateTime.now();

    public boolean isHazardous() {
        if (Boolean.TRUE.equals(hazardous)) return true;
        if ("BATTERY".equalsIgnoreCase(type)) return true;
        if (name != null) {
            String lower = name.toLowerCase();
            return lower.contains("pin ") || lower.startsWith("pin") || lower.contains("lipo") || lower.contains("battery") || lower.contains("ắc quy") || lower.contains("ac quy") || lower.contains("cháy nổ");
        }
        return false;
    }

    public long getEffectiveHazardousFee() {
        if (!isHazardous()) return 0L;
        return (hazardousFee != null && hazardousFee > 0) ? hazardousFee : 25_000L;
    }
}
