package com.ares.shop.model;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity
public class Referral {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true) public String code;
    public Long ownerUserId; public int rewardPoints = 50; public int uses; public int maxUses = 100;
    public LocalDateTime createdAt = LocalDateTime.now();
}
