package com.ares.shop.model;
import jakarta.persistence.*; import java.time.LocalDateTime;
/** Hộp thư mô phỏng: hoá đơn, nhắc giỏ hàng, tồn kho thấp, báo có hàng… (không gửi thật). */
@Entity
public class EmailLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String toAddr; public String subject; public String kind;
    @Lob @Column(length = 20000) public String body;
    public LocalDateTime createdAt = LocalDateTime.now();
}
