package com.ares.shop.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
/** Giao dịch cổng thanh toán (VNPay/MoMo sandbox): PENDING → SUCCESS | FAILED | CANCELLED | EXPIRED. */
@Entity
public class PaymentTxn {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true) public String txnRef;
    public String orderCode; public String provider; public long amount;
    public String status = "PENDING"; public String responseCode; public String bankCode; public String gatewayTxnNo;
    public int ipnCount; public LocalDateTime createdAt = LocalDateTime.now(), expiresAt, completedAt;
}
