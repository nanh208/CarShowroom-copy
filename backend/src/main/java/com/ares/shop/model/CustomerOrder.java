package com.ares.shop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity @Table(name = "orders")
public class CustomerOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true) public String code;
    @JsonIgnoreProperties({"password"}) @ManyToOne(fetch = FetchType.EAGER) public User user;
    public String buyerName;
    public String phone;
    public String email;
    public String shippingAddress;
    @Column(length = 1000) public String note;
    /** Gói quà (20.000đ) + lời nhắn kèm thiệp. Kiểu bọc để tương thích DB cũ (ddl-auto=update). */
    public Boolean giftWrap; @Column(length = 300) public String giftMessage; public Long giftFee;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    public List<OrderItem> items = new ArrayList<>();
    /** v11: phương thức giao, ngày giao dự kiến, combo, tín dụng, hoàn tiền một phần, khách vãng lai. */
    public String shippingMethod; public java.time.LocalDate etaFrom; public java.time.LocalDate etaTo;
    public Long bundleDiscount; public Long creditUsed; public Long refundedAmount; public Boolean guest;
    public long subtotal;
    public long discount;
    public long pointsDiscount;
    public long shippingFee;
    public long total;
    public String promoCode;
    @Enumerated(EnumType.STRING) public Enums.PaymentMethod paymentMethod;
    @Enumerated(EnumType.STRING) public Enums.PaymentStatus paymentStatus = Enums.PaymentStatus.UNPAID;
    public String city;
    public long paidAmount;
    public int pointsUsed;
    public int pointsEarned;
    public int installmentMonths;
    public long monthlyInstallment;
    public String transactionRef;
    @Enumerated(EnumType.STRING) public Enums.OrderStatus status = Enums.OrderStatus.PENDING;
    public LocalDateTime createdAt = LocalDateTime.now();
    public LocalDateTime paidAt;
    public String cancelReason;
    /** Mốc thời gian từng trạng thái để hiển thị timeline theo dõi đơn. */
    @ElementCollection(fetch = FetchType.EAGER) public Map<String, LocalDateTime> timeline = new LinkedHashMap<>();
}
