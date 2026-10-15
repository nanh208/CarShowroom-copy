package com.ares.shop.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
/** Tuỳ chọn cá nhân lưu ở server (so sánh, widget dashboard…), thay cho localStorage. */
@Entity @Table(uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "k"}))
public class UserPref {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long userId; public String k; @Column(length = 4000) public String v; public LocalDateTime updatedAt = LocalDateTime.now();
}
