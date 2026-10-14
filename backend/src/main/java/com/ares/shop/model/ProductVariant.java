package com.ares.shop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/** Biến thể (màu / phiên bản) với giá và tồn kho riêng. */
@Entity
public class ProductVariant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @JsonIgnore @ManyToOne(optional = false) public Product product;
    public String name;
    public String color;   // hex, null nếu là phiên bản
    public long price;
    public Long oldPrice;
    public int stock;
    public String image;
    public boolean active = true;
}
