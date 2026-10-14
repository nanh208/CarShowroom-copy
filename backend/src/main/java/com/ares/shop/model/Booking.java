package com.ares.shop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.*;

@Entity
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @JsonIgnoreProperties({"password"}) @ManyToOne(fetch = FetchType.EAGER) public User user;
    @Enumerated(EnumType.STRING) public Enums.BookingType type;
    public String name;
    public String phone;
    public String email;
    public Long productId;
    public String productName;
    public String serviceType;      // Vệ sinh, sơn lại, thay bánh, sửa đèn...
    public LocalDate date;
    public String timeSlot;
    @Column(length = 1000) public String note;
    @Enumerated(EnumType.STRING) public Enums.BookingStatus status = Enums.BookingStatus.PENDING;
    public LocalDateTime createdAt = LocalDateTime.now();
}
