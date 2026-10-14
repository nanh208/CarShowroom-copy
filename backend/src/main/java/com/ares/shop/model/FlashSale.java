package com.ares.shop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
public class FlashSale {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long productId; public Long variantId; public String productName; public long salePrice; public int quota; public int sold; public LocalDateTime startTime; public LocalDateTime endTime; public boolean active = true;
    public boolean isLive() { var n = LocalDateTime.now(); return active && sold < quota && !n.isBefore(startTime) && n.isBefore(endTime); }
}
