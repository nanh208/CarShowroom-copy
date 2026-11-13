package com.ares.shop.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_debts")
public class StaffDebt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** ID nhân viên nợ */
    public Long staffId;

    /** Họ tên nhân viên */
    public String staffName;

    /** Email nhân viên */
    public String staffEmail;

    /**
     * Loại công nợ:
     * - TAM_UNG: Tạm ứng lương
     * - COD_THIEU: Thiếu tiền COD giao hàng
     * - MUA_HANG: Mua hàng nội bộ
     * - DEN_BU: Làm hỏng hàng / Bồi thường tài sản
     * - KHAC: Khác
     */
    public String debtType = "TAM_UNG";

    /** Tổng số tiền nợ */
    public long amount;

    /** Số tiền đã thanh toán / đã cấn trừ */
    public long paidAmount = 0;

    /** Số tiền còn lại phải trả */
    public long remainingAmount;

    /**
     * Trạng thái công nợ:
     * - UNPAID: Chưa trả
     * - PARTIAL: Đã trả một phần
     * - PAID: Đã hoàn tất
     */
    public String status = "UNPAID";

    /** Ghi chú / Lý do nợ */
    @Column(length = 2000)
    public String reason;

    /** Hạn chót thanh toán */
    public LocalDate dueDate;

    /** Ngày tạo */
    public LocalDateTime createdAt = LocalDateTime.now();

    /** Ngày cập nhật gần nhất */
    public LocalDateTime updatedAt = LocalDateTime.now();

    /** Người tạo / Người phê duyệt (Admin/Kế toán) */
    public String createdBy;

    /** Lịch sử các lần cấn trừ / trả nợ (JSON) */
    @Column(length = 4000)
    public String paymentHistoryJson;
}
