package com.ares.shop.service;

import com.ares.shop.model.CustomerOrder;
import com.ares.shop.service.payment.Hmac;
import com.ares.shop.service.payment.PaymentProps;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Cổng VNPay 2.1.0. Đơn CHỈ được xác nhận qua IPN có vnp_SecureHash (HMAC-SHA512) hợp lệ.
 * Key lấy từ VNPAY_TMN_CODE / VNPAY_HASH_SECRET.
 */
@Service
public class VnPayService {
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final PaymentProps props;

    public VnPayService(PaymentProps props) { this.props = props; }

    public boolean ready() { return props.vnpayReady(); }

    /** Chuỗi dữ liệu ký: key sắp xếp alphabet, bỏ giá trị rỗng, value URL-encode (US-ASCII) như code mẫu VNPay. */
    public static String hashData(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>();
        params.forEach((k, v) -> {
            if (k == null || v == null || v.isEmpty()) return;
            if (k.equals("vnp_SecureHash") || k.equals("vnp_SecureHashType")) return;
            if (!k.startsWith("vnp_")) return;
            sorted.put(k, v);
        });
        StringJoiner j = new StringJoiner("&");
        sorted.forEach((k, v) -> j.add(k + "=" + URLEncoder.encode(v, StandardCharsets.US_ASCII)));
        return j.toString();
    }

    public static String sign(Map<String, String> params, String secret) { return Hmac.sha512Hex(secret, hashData(params)); }

    /** Tạo URL thanh toán VNPay. */
    public String createPaymentUrl(CustomerOrder order, long amount, String ipAddress, String returnUrl) {
        if (!ready()) throw new IllegalStateException("VNPay chưa được cấu hình");
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        Map<String, String> p = new HashMap<>();
        p.put("vnp_Version", "2.1.0");
        p.put("vnp_Command", "pay");
        p.put("vnp_TmnCode", props.vnpayTmnCode);
        p.put("vnp_Amount", String.valueOf(amount * 100L));
        p.put("vnp_CurrCode", "VND");
        p.put("vnp_TxnRef", order.code);
        p.put("vnp_OrderInfo", "Thanh toan don hang " + order.code);
        p.put("vnp_OrderType", "other");
        p.put("vnp_Locale", "vn");
        p.put("vnp_ReturnUrl", returnUrl);
        p.put("vnp_IpAddr", ipAddress == null || ipAddress.isBlank() ? "127.0.0.1" : ipAddress);
        p.put("vnp_CreateDate", F.format(now));
        p.put("vnp_ExpireDate", F.format(now.plusMinutes(15)));
        String data = hashData(p);
        return props.vnpayPayUrl + "?" + data + "&vnp_SecureHash=" + Hmac.sha512Hex(props.vnpayHashSecret, data);
    }

    /** URL trả về mặc định: backend kiểm chữ ký rồi chuyển về trang /pay/:code (chỉ hiển thị, không xác nhận tiền). */
    public String createPaymentUrl(CustomerOrder order, long amount, String ipAddress) {
        return createPaymentUrl(order, amount, ipAddress, props.backendBase() + "/api/payment/vnpay/return");
    }

    /** Xác thực chữ ký + mã merchant của IPN / return URL. */
    public boolean verify(Map<String, String> fields) {
        if (!ready()) return false;
        String got = fields.get("vnp_SecureHash");
        if (got == null || got.isBlank()) return false;
        if (!props.vnpayTmnCode.equals(fields.get("vnp_TmnCode"))) return false;
        return Hmac.equalsHex(sign(fields, props.vnpayHashSecret), got);
    }
}
