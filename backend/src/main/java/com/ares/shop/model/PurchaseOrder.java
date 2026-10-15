package com.ares.shop.model;
import jakarta.persistence.*; import java.time.LocalDateTime;
/** Đơn đặt hàng nhà cung cấp / gợi ý nhập hàng. */
@Entity
public class PurchaseOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String supplier = "Nhà cung cấp mặc định";
    @Lob @Column(length = 8000) public String itemsJson; // [{productId,sku,name,qty,unitCost}]
    public String status = "DRAFT"; // DRAFT|ORDERED|RECEIVED|CANCELLED
    public long totalCost; public String note;
    public LocalDateTime createdAt = LocalDateTime.now(); public LocalDateTime receivedAt;
}
