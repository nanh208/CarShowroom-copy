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
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    public List<OrderItem> items = new ArrayList<>();
    public long subtotal;
    public long discount;
    public long shippingFee;
    public long total;
    public String promoCode;
    @Enumerated(EnumType.STRING) public Enums.PaymentMethod paymentMethod;
    @Enumerated(EnumType.STRING) public Enums.PaymentStatus paymentStatus = Enums.PaymentStatus.UNPAID;
    public long paidAmount;
    public int installmentMonths;
    public long monthlyInstallment;
    public String transactionRef;
    @Enumerated(EnumType.STRING) public Enums.OrderStatus status = Enums.OrderStatus.PENDING;
    public LocalDateTime createdAt = LocalDateTime.now();
    /** Mốc thời gian từng trạng thái để hiển thị timeline theo dõi đơn. */
    @ElementCollection(fetch = FetchType.EAGER) public Map<String, LocalDateTime> timeline = new LinkedHashMap<>();
}
