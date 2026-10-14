package com.ares.shop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @JsonIgnore @ManyToOne public CustomerOrder order;
    public Long productId;
    public Long variantId;
    public String productName;
    public String variantName;
    public String image;
    public String sku;
    public long unitPrice;
    public int quantity;
    public int warrantyMonths;
    public boolean flashSale;
    @Column(length = 500) public String options;
}
