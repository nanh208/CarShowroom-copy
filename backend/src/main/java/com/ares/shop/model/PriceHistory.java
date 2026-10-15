package com.ares.shop.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
/** Lịch sử giá sản phẩm (ghi khi giá thay đổi) — phục vụ biểu đồ giá + "thấp nhất 30 ngày". */
@Entity @Table(indexes = @Index(columnList = "productId"))
public class PriceHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long productId; public long price; public LocalDateTime at = LocalDateTime.now();
}
