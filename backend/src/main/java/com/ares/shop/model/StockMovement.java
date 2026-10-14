package com.ares.shop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
public class StockMovement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long productId; public Long variantId; public String productName; public int delta; public int after; public String reason; public String byUser; public LocalDateTime createdAt = LocalDateTime.now();
}
