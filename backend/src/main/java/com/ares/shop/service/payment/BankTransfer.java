package com.ares.shop.service.payment;

/** Giao dịch tiền vào đã chuẩn hoá từ một nhà cung cấp webhook ngân hàng (SePay / payOS / Casso). */
public record BankTransfer(String provider, String externalId, String content, long amount, boolean incoming, String accountNumber) {}
