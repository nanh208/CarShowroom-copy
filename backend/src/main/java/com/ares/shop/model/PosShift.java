package com.ares.shop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pos_shifts")
public class PosShift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** Mã ca làm việc: CA-20261109-001 */
    @Column(unique = true, nullable = false)
    public String shiftCode;

    /** ID & Tên nhân viên trực quầy / thu ngân */
    public Long staffId;
    public String staffName;
    public String staffEmail;

    /** Thời gian bắt đầu và kết thúc ca */
    public LocalDateTime startTime = LocalDateTime.now();
    public LocalDateTime endTime;

    /** Tiền mặt ban đầu trong két thu ngân */
    public long openingCash = 0L;

    /** Tiền mặt thực tế đếm được khi bàn giao / chốt ca */
    public Long closingCash;

    /** Tiền mặt dự kiến có trong két theo hệ thống */
    public long expectedCash = 0L;

    /** Chênh lệch tiền két (thừa dương, thiếu âm) */
    public Long cashDifference = 0L;

    /** Doanh thu & sản lượng phát sinh trong ca */
    public long totalSales = 0L;
    public int orderCount = 0;
    public int itemCount = 0;

    /** Doanh thu phân theo phương thức thanh toán trong ca */
    public long cashSales = 0L;
    public long bankQrSales = 0L;
    public long cardSales = 0L;
    public long otherSales = 0L;

    /** Trạng thái ca: OPEN (đang mở) hoặc CLOSED (đã đóng) */
    public String status = "OPEN";

    /** Ghi chú bàn giao ca */
    @Column(length = 2000)
    public String notes;

    public LocalDateTime createdAt = LocalDateTime.now();
}
