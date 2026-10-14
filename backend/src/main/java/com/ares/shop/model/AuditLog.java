package com.ares.shop.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
/** Nhật ký thao tác của nhân viên/quản trị (mọi thay đổi qua /api/admin). */
@Entity
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String actor; public String role; public String method; public String path; public Integer status;
    @Column(length = 500) public String summary;
    public LocalDateTime createdAt = LocalDateTime.now();
}
