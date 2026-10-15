package com.ares.shop.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
/** Sản phẩm đã xem gần đây (theo tài khoản). */
@Entity @Table(uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "productId"}))
public class ProductView {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long userId; public Long productId; public LocalDateTime viewedAt = LocalDateTime.now(); public Integer count = 1;
}
