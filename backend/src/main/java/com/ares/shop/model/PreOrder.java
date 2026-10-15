package com.ares.shop.model;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Đăng ký đặt trước mẫu sắp ra mắt. Trạng thái: WAITING → NOTIFIED → CONVERTED | CANCELLED. */
@Entity
public class PreOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long productId; public String productName; public Long userId; public String name; public String email; public String phone;
    public Integer qty = 1; public Long deposit = 0L; public String status = "WAITING"; public String note;
    public LocalDateTime createdAt = LocalDateTime.now(); public LocalDateTime notifiedAt;
}
