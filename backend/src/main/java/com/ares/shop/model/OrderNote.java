package com.ares.shop.model;
import jakarta.persistence.*; import java.time.LocalDateTime;
/** Ghi chú nội bộ trên đơn (khách không thấy). */
@Entity
public class OrderNote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long orderId; public String author; @Column(length = 2000) public String text;
    public LocalDateTime createdAt = LocalDateTime.now();
}
