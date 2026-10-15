package com.ares.shop.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
/** Quy trình xuất kho nhiều bước cho 1 đơn: PICKING → PACKED → SHIPPED (→ DELIVERED). */
@Entity
public class Shipment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true) public Long orderId; public String orderCode;
    public String stage = "PICKING";
    /** itemId → số lượng đã nhặt */
    @ElementCollection(fetch = FetchType.EAGER) public Map<Long, Integer> picked = new HashMap<>();
    public String pickedBy; public LocalDateTime pickStartedAt, pickedAt;
    public String packedBy; public LocalDateTime packedAt; public Integer weightGrams; public String boxSize; public Integer packages;
    public String carrier, service, trackingNo; public Long fee, codFee, codAmount; public String shippedBy; public LocalDateTime shippedAt, deliveredAt;
    public LocalDateTime createdAt = LocalDateTime.now();
}
