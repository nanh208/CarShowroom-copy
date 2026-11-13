package com.ares.shop.service.payment;

import java.util.List;
import java.util.Map;

/**
 * Hợp đồng chung cho các nhà cung cấp webhook biến động số dư. Thêm nhà cung cấp mới = thêm 1 lớp implement.
 */
public interface BankWebhookProvider {
    /** Tên ngắn: SEPAY, PAYOS, CASSO. */
    String name();
    boolean enabled();
    /** Xác thực request (API key / chữ ký). headers: tên header viết thường. */
    boolean verify(Map<String, String> headers, String rawBody);
    /** Bóc tách các giao dịch từ body (đã xác thực). */
    List<BankTransfer> parse(String rawBody) throws Exception;
}
