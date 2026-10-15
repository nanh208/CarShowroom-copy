package com.ares.shop.model;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity
public class AbandonedCart {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long userId; public String email; public String name;
    @Lob @Column(length = 8000) public String itemsJson;
    public long subtotal; public LocalDateTime lastSeen = LocalDateTime.now();
}
