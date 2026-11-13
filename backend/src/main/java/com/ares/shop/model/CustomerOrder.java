package com.ares.shop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity @Table(name = "orders")
public class CustomerOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true) public String code;
    @JsonIgnoreProperties({"password"}) @ManyToOne(fetch = FetchType.EAGER) public User user;
    public String buyerName;
    public String phone;
    public String email;
    public String shippingAddress;
    @Column(length = 1000) public String note;
    /** Gói quà (20.000đ) + lời nhắn kèm thiệp. Kiểu bọc để tương thích DB cũ (ddl-auto=update). */
    public Boolean giftWrap; @Column(length = 300) public String giftMessage; public Long giftFee;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    public List<OrderItem> items = new ArrayList<>();
    /** v11: phương thức giao, ngày giao dự kiến, combo, tín dụng, hoàn tiền một phần, khách vãng lai. */
    public String shippingMethod; public java.time.LocalDate etaFrom; public java.time.LocalDate etaTo;
    public Double shippingDistanceKm; public String carrierName;
    public Long bundleDiscount; public Long creditUsed; public Long refundedAmount; public Boolean guest;
    public long subtotal;
    public long discount;
    public long pointsDiscount;
    public long shippingFee;
    public long total;
    public String promoCode;
    @Enumerated(EnumType.STRING) public Enums.PaymentMethod paymentMethod;
    @Enumerated(EnumType.STRING) public Enums.PaymentStatus paymentStatus = Enums.PaymentStatus.UNPAID;
    public String city;
    public long paidAmount;
    public int pointsUsed;
    public int pointsEarned;
    public int installmentMonths;
    public long monthlyInstallment;
    public String transactionRef;
    @Enumerated(EnumType.STRING) public Enums.OrderStatus status = Enums.OrderStatus.PENDING;
    public LocalDateTime createdAt = LocalDateTime.now();
    public LocalDateTime paidAt;
    public String cancelReason;
    /** Phụ phí an toàn vận chuyển cho hàng cháy nổ / Pin LiPo (mặc định 25.000đ). */
    public Long hazardousFee;
    public Boolean hasHazardous;
    @Column(length = 500) public String hazardousNote;

    /** Kênh bán hàng: ONLINE (website/app) hoặc POS (bán trực tiếp tại showroom quầy). */
    public String channel = "ONLINE";

    /** Nhân viên bán hàng / tư vấn phụ trách đơn. */
    public Long staffId;
    public String staffName;

    /** Thưởng hoa hồng nhân viên cho đơn này. */
    public Long commissionAmount = 0L;
    public Double commissionRate = 0.0;
    public String commissionStatus = "PENDING"; // PENDING, APPROVED, PAID

    /** Nghiệp vụ POS bán tại quầy. */
    public String posPaymentMethod;
    public Long cashGiven = 0L;
    public Long cashChange = 0L;
    public Long shiftId;

    /** Xuất hóa đơn VAT điện tử cho khách hàng / doanh nghiệp tại quầy. */
    public Boolean requireVatInvoice = false;
    public String companyName;
    public String companyTaxCode;
    public String companyAddress;
    public String companyEmail;
    public Long vatAmount = 0L;

    /** Thẩm quyền quản lý: Phê duyệt chiết khấu đặc quyền & Ghi chú quản lý. */
    public Long managerDiscountAmount = 0L;
    public String managerDiscountReason;
    public String managerApprovedBy;

    /** Mốc thời gian từng trạng thái để hiển thị timeline theo dõi đơn. */
    @ElementCollection(fetch = FetchType.EAGER) public Map<String, LocalDateTime> timeline = new LinkedHashMap<>();
}
