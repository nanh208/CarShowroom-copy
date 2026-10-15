package com.ares.shop.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
/** Đăng ký mua định kỳ: hệ thống tự tạo đơn theo chu kỳ (@Scheduled). */
@Entity
public class ProductSubscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @JsonIgnoreProperties({"password", "tags"}) @ManyToOne(fetch = FetchType.EAGER) public User user;
    public Long productId; public Long variantId; public String productName; public String image; public long unitPrice;
    public int quantity = 1; public int intervalDays = 30; public int discountPct = 5;
    public String status = "ACTIVE"; // ACTIVE | PAUSED | CANCELLED
    public LocalDateTime nextRunAt; public LocalDateTime lastRunAt; public LocalDateTime createdAt = LocalDateTime.now();
    public String buyerName; public String phone; public String shippingAddress; public String city;
    @Enumerated(EnumType.STRING) public Enums.PaymentMethod paymentMethod = Enums.PaymentMethod.COD;
    public int runs; public int failCount; public String lastOrderCode; @Column(length = 500) public String lastError;
}
