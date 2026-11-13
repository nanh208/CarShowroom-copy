package com.ares.shop.service.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Toàn bộ cấu hình thanh toán, đọc từ biến môi trường qua application.properties. */
@Component
public class PaymentProps {
    // URL công khai
    @Value("${app.frontend-url}") public String frontendUrl;
    @Value("${app.payment.public-base-url:}") public String publicBaseUrl;

    // Tài khoản nhận chuyển khoản (VietQR)
    @Value("${app.payment.bank.bin:}") public String bankBin;
    @Value("${app.payment.bank.code:}") public String bankCode;
    @Value("${app.payment.bank.name:}") public String bankName;
    @Value("${app.payment.bank.account-no:}") public String bankAccountNo;
    @Value("${app.payment.bank.account-name:}") public String bankAccountName;
    @Value("${app.payment.bank.qr-template:compact2}") public String qrTemplate;

    // SePay
    @Value("${app.payment.sepay.enabled:false}") public boolean sepayEnabled;
    @Value("${app.payment.sepay.api-key:}") public String sepayApiKey;

    // payOS
    @Value("${app.payment.payos.enabled:false}") public boolean payosEnabled;
    @Value("${app.payment.payos.client-id:}") public String payosClientId;
    @Value("${app.payment.payos.api-key:}") public String payosApiKey;
    @Value("${app.payment.payos.checksum-key:}") public String payosChecksumKey;
    @Value("${app.payment.payos.endpoint:https://api-merchant.payos.vn}") public String payosEndpoint;

    // Casso
    @Value("${app.payment.casso.enabled:false}") public boolean cassoEnabled;
    @Value("${app.payment.casso.checksum-key:}") public String cassoChecksumKey;
    @Value("${app.payment.casso.secure-token:}") public String cassoSecureToken;
    @Value("${app.payment.casso.max-age-seconds:300}") public long cassoMaxAgeSeconds;

    // MoMo
    @Value("${app.payment.momo.enabled:false}") public boolean momoEnabled;
    @Value("${app.payment.momo.endpoint:https://test-payment.momo.vn}") public String momoEndpoint;
    @Value("${app.payment.momo.partner-code:}") public String momoPartnerCode;
    @Value("${app.payment.momo.access-key:}") public String momoAccessKey;
    @Value("${app.payment.momo.secret-key:}") public String momoSecretKey;

    // VNPay
    @Value("${app.payment.vnpay.enabled:false}") public boolean vnpayEnabled;
    @Value("${app.payment.vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") public String vnpayPayUrl;
    @Value("${app.payment.vnpay.tmn-code:}") public String vnpayTmnCode;
    @Value("${app.payment.vnpay.hash-secret:}") public String vnpayHashSecret;

    public boolean bankConfigured() { return !isBlank(bankBin) && !isBlank(bankAccountNo); }
    public boolean anyBankWebhook() { return sepayEnabled || payosEnabled || cassoEnabled; }
    public boolean momoReady() { return momoEnabled && !isBlank(momoPartnerCode) && !isBlank(momoAccessKey) && !isBlank(momoSecretKey); }
    public boolean vnpayReady() { return vnpayEnabled && !isBlank(vnpayTmnCode) && !isBlank(vnpayHashSecret); }
    public boolean payosReady() { return payosEnabled && !isBlank(payosClientId) && !isBlank(payosApiKey) && !isBlank(payosChecksumKey); }

    /** Gốc URL backend công khai (dùng cho IPN). Mặc định = OAUTH_REDIRECT_BASE / localhost. */
    public String backendBase() {
        String b = isBlank(publicBaseUrl) ? "http://localhost:8080" : publicBaseUrl;
        return b.endsWith("/") ? b.substring(0, b.length() - 1) : b;
    }

    /** Ảnh VietQR (img.vietqr.io) – cùng nguồn cấu hình với thông tin hiển thị. */
    public String vietQrUrl(String orderCode, long amount) {
        if (!bankConfigured()) return "";
        return "https://img.vietqr.io/image/" + enc(bankBin.trim()) + "-" + enc(bankAccountNo.trim()) + "-" + enc(qrTemplate) + ".png"
                + "?amount=" + Math.max(0, amount) + "&addInfo=" + enc(orderCode) + "&accountName=" + enc(bankAccountName == null ? "" : bankAccountName);
    }

    /** Thông tin tài khoản nhận + QR dùng chung cho mọi màn hình. */
    public java.util.Map<String, Object> bankInfo(String orderCode, long amount) {
        var m = new java.util.LinkedHashMap<String, Object>();
        m.put("configured", bankConfigured());
        m.put("bank", isBlank(bankName) ? "VietinBank" : bankName);
        m.put("bin", bankBin);
        m.put("account", bankAccountNo);
        m.put("holder", bankAccountName);
        m.put("content", orderCode);
        m.put("amount", Math.max(0, amount));
        m.put("qr", vietQrUrl(orderCode, amount));
        m.put("autoConfirm", anyBankWebhook());
        return m;
    }

    private static String enc(String s) { return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"); }

    public static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
