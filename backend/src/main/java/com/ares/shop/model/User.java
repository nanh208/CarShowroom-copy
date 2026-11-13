package com.ares.shop.model;

import java.time.*;
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
    /** Mã số thẻ VIP định danh (VD: 8899-2026-0001-1200). */
    public String vipCardNumber;
    public LocalDateTime createdAt = LocalDateTime.now();
    /** Ghi chú nội bộ + nhãn CRM (chỉ nhân viên thấy). */
    @JsonIgnore @Column(length = 2000) public String staffNote;
    @JsonIgnore @ElementCollection(fetch = FetchType.EAGER) public Set<String> tags = new LinkedHashSet<>();
    /** Phương thức thanh toán ưa thích. */
    public String preferredPayment;
    /** Tín dụng cửa hàng (từ thẻ quà / hoàn tiền), mã giới thiệu, 2FA TOTP (tuỳ chọn). */
    public Long storeCredit; public Long referredBy;
    @JsonIgnore public String totpSecret; public Boolean totpEnabled;
    /** Phần thưởng: sinh nhật, điểm danh hằng ngày, vòng quay may mắn. */
    public LocalDate birthday; public Integer birthdayYearClaimed; public LocalDate lastCheckin; public Integer checkinStreak; public LocalDate lastSpin;

    /** Thông tin nhân sự & tính thưởng hoa hồng (STAFF / ADMIN). */
    public String staffCode;
    public String department;
    public Double commissionRate = 2.0; // Tỷ lệ hoa hồng mặc định (%)
    public Long monthlySalesTarget = 50_000_000L; // Chỉ tiêu doanh số 50 triệu/tháng
    public String employmentStatus = "ACTIVE"; // ACTIVE, PROBATION, LEAVE, TERMINATED
    public LocalDate hireDate = LocalDate.now();
    public Long totalCommissionPaid = 0L; // Tổng hoa hồng đã quyết toán chi trả

    // Định danh đăng nhập mạng xã hội (liên kết theo ID nhà cung cấp, không dựa vào email giả)
    @JsonIgnore @Column(unique = true) public String googleId;
    @JsonIgnore @Column(unique = true) public String facebookId;
}
