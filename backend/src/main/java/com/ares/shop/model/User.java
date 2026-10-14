package com.ares.shop.model;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true, nullable = false) public String email;
    @JsonIgnore public String password;
    public String fullName;
    public String phone;
    public String avatarUrl;
    @Enumerated(EnumType.STRING) public Enums.Role role = Enums.Role.USER;
    public String provider = "LOCAL";
    public boolean locked = false;
    public int points;
    public String tier = "Đồng";
    public LocalDateTime createdAt = LocalDateTime.now();
    /** Ghi chú nội bộ + nhãn CRM (chỉ nhân viên thấy). */
    @JsonIgnore @Column(length = 2000) public String staffNote;
    @JsonIgnore @ElementCollection(fetch = FetchType.EAGER) public Set<String> tags = new LinkedHashSet<>();
    /** Phương thức thanh toán ưa thích. */
    public String preferredPayment;
}
