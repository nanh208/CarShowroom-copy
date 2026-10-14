package com.ares.shop.model;

public final class Enums {
    private Enums() {}
    public enum Role { USER, ADMIN }
    public enum ProductType { SUPERCAR, SEDAN, SUV, CLASSIC, RACING, TRUCK, ACCESSORY }
    public enum PaymentMethod { FULL, DEPOSIT, INSTALLMENT, COD }
    public enum PaymentStatus { UNPAID, DEPOSITED, PARTIAL, PAID }
    public enum OrderStatus { PENDING, CONFIRMED, SHIPPING, COMPLETED, CANCELLED }
    public enum BookingType { VIEWING, MAINTENANCE }
    public enum BookingStatus { PENDING, CONFIRMED, DONE, CANCELLED }
}
