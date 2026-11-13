package com.ares.shop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_commission_tx")
public class StaffCommissionTx {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public Long staffId;
    public String staffName;

    /** Loại giao dịch hoa hồng: EARN (Cộng thưởng từ đơn hàng), PAYOUT (Chi trả hoa hồng), ADJUST (Điều chỉnh) */
    public String txType = "EARN";

    public Long orderId;
    public String orderCode;

    public long amount = 0L;
    public double rate = 0.0;

    @Column(length = 1000)
    public String note;

    public String createdBy;
    public LocalDateTime createdAt = LocalDateTime.now();
}
