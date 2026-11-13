package com.ares.shop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Nhật ký giao dịch tiền vào từ webhook/IPN. (provider, externalId) là duy nhất => chống xử lý trùng (idempotent). */
@Entity
@Table(name = "payment_transactions",
       uniqueConstraints = @UniqueConstraint(name = "uk_payment_tx_provider_ext", columnNames = {"provider", "externalId"}))
public class PaymentTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, length = 20) public String provider;      // SEPAY, PAYOS, CASSO, MOMO, VNPAY
    @Column(nullable = false, length = 120) public String externalId;   // id giao dịch phía nhà cung cấp
    public String orderCode;
    public long amount;
    @Column(length = 40) public String result;                          // APPLIED, UNDERPAID, NO_ORDER, ALREADY_PAID, ORDER_CANCELLED, FAILED
    @Column(length = 500) public String content;
    @Lob @Column(columnDefinition = "TEXT") public String rawPayload;
    public LocalDateTime createdAt = LocalDateTime.now();
}
