package com.ares.shop.service;

/** Nhà cung cấp SMS có thể thay thế: mock (dev, hộp thư sandbox) hoặc twilio (env). Thêm nhà cung cấp mới = thêm bean triển khai + app.sms.provider. */
public interface SmsProvider {
    String name();
    void send(String phoneE164, String text) throws Exception;
}
