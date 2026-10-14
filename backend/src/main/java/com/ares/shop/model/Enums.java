package com.ares.shop.model;

public final class Enums {
    private Enums() {}
    public enum Role { USER, STAFF, ADMIN }
    public enum PaymentMethod { COD, BANK_QR, MOMO, VNPAY, INSTALLMENT }
    public enum PaymentStatus { UNPAID, PAID, PARTIAL, REFUNDED }
    public enum OrderStatus { PENDING, CONFIRMED, SHIPPING, COMPLETED, CANCELLED, RETURNED }
    public enum AuctionStatus { SCHEDULED, LIVE, ENDED, CANCELLED }
    public enum ReturnStatus { PENDING, APPROVED, REJECTED, REFUNDED }
    public enum TicketStatus { OPEN, ANSWERED, CLOSED }
    public enum VoucherKind { PERCENT, FIXED, FREESHIP }
}
