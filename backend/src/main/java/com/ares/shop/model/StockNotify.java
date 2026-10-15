package com.ares.shop.model;
import jakarta.persistence.*; import java.time.LocalDateTime;
/** "Báo tôi khi có hàng" theo email (khách vãng lai hoặc đã đăng nhập). */
@Entity
public class StockNotify {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long productId; public String email; public Boolean sent;
    public LocalDateTime createdAt = LocalDateTime.now();
}
