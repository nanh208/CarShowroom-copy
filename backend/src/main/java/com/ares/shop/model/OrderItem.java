package com.ares.shop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @JsonIgnore @ManyToOne public CustomerOrder order;
    public Long productId;
    public String productName;
    public String scale;
    public long unitPrice;
    public int quantity;
    @Column(length = 500) public String options;   // "Sơn: Đỏ; Mâm: Vàng; Nội thất: Đen"
}
