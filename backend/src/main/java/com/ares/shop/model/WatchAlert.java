package com.ares.shop.model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
/** Đăng ký báo giảm giá (PRICE) / có hàng lại (STOCK) cho sản phẩm yêu thích. */
@Entity
public class WatchAlert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @JsonIgnore @ManyToOne(fetch = FetchType.EAGER) public User user;
    public Long productId; public String kind; public Long basePrice; public Boolean fired;
    public LocalDateTime createdAt = LocalDateTime.now();
}
