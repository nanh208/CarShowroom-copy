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
    public LocalDateTime createdAt = LocalDateTime.now();
}
