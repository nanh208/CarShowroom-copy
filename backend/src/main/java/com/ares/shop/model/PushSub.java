package com.ares.shop.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
/** Đăng ký Web Push (PWA) của trình duyệt. */
@Entity
public class PushSub {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long userId; @Column(length = 600, unique = true) public String endpoint; public String p256dh; public String auth;
    public LocalDateTime createdAt = LocalDateTime.now(); public LocalDateTime lastSentAt; public int sent; public int failures; public String lastStatus;
}
