package com.ares.shop.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
/** Đối tác tiếp thị liên kết: mã giới thiệu + % hoa hồng. */
@Entity
public class Affiliate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @JsonIgnoreProperties({"password", "tags"}) @OneToOne(fetch = FetchType.EAGER) public User user;
    @Column(unique = true) public String code;
    public double commissionPct = 5; public String status = "ACTIVE"; // ACTIVE | SUSPENDED
    public long clicks; @Column(length = 300) public String payoutInfo; public LocalDateTime createdAt = LocalDateTime.now();
}
