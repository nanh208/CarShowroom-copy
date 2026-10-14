package com.ares.shop.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Promotion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true) public String code;
    public String title;
    @Column(length = 1000) public String description;
    @Enumerated(EnumType.STRING) public Enums.VoucherKind kind = Enums.VoucherKind.PERCENT;
    public int discountPercent;
    public long amount;          // giảm cố định (FIXED)
    public int usageLimit;       // 0 = không giới hạn
    public int usedCount;
    public boolean isPublic = true;
    public long maxDiscount;      // 0 = không giới hạn
    public long minOrder;
    public LocalDate startDate;
    public LocalDate endDate;
    public boolean active = true;
    public String bannerColor = "#0284c7";

    public boolean isValidNow() {
        LocalDate t = LocalDate.now();
        return active && (usageLimit == 0 || usedCount < usageLimit) && (startDate == null || !t.isBefore(startDate)) && (endDate == null || !t.isAfter(endDate));
    }
}
