package com.ares.shop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String name;
    @ManyToOne(fetch = FetchType.EAGER) public Brand brand;
    @Enumerated(EnumType.STRING) public Enums.ProductType type;
    public String fuel;            // Xăng, Dầu, Hybrid, Điện
    public long price;              // VND
    public Long oldPrice;
    public int stock;
    public int sold;
    @Column(name = "model_year") public Integer year;
    public String material;         // Hộp số / ghi chú ngắn
    public String baseColor = "#c1121f";
    @Column(length = 3000) public String description;
    @ElementCollection(fetch = FetchType.EAGER) @OrderColumn
    public List<String> images = new ArrayList<>();
    @ElementCollection(fetch = FetchType.EAGER) @MapKeyColumn(name = "spec_key") @Column(name = "spec_value")
    public Map<String, String> specs = new LinkedHashMap<>();
    public boolean isNew;
    public boolean featured;
    public boolean customizable = true;
    public boolean active = true;
    public LocalDateTime createdAt = LocalDateTime.now();
}
