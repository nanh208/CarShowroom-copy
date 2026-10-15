package com.ares.shop.model;
import jakarta.persistence.*; import java.util.*;
/** Combo: BUY_X_GET_Y hoặc giá gói cố định (FIXED). */
@Entity
public class Bundle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String title; @Column(length = 1000) public String description;
    public String kind = "BUY_X_GET_Y"; // BUY_X_GET_Y | FIXED
    public int buyQty = 2; public int getQty = 1; public long bundlePrice;
    @ElementCollection(fetch = FetchType.EAGER) public List<Long> productIds = new ArrayList<>();
    public boolean active = true; public int sortOrder;
}
