package com.ares.shop.model;
import jakarta.persistence.*; import java.time.LocalDateTime;
/** Thẻ quà / tín dụng cửa hàng. */
@Entity
public class GiftCard {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true) public String code;
    public long initial; public long balance;
    public Long buyerUserId; public Long redeemedByUserId;
    public String message; public boolean active = true;
    public LocalDateTime expiresAt; public LocalDateTime createdAt = LocalDateTime.now();
}
