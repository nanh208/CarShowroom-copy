package com.ares.shop.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
/** Hoa hồng theo đơn: PENDING → APPROVED (đơn đã giao) → PAID; VOID khi đơn huỷ/trả. */
@Entity
public class AffiliateCommission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long affiliateId; public String affiliateCode; @Column(unique = true) public String orderCode;
    public long base; public double pct; public long amount; public String status = "PENDING";
    public LocalDateTime createdAt = LocalDateTime.now(); public LocalDateTime approvedAt; public LocalDateTime paidAt;
}
