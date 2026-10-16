package com.ares.shop.service;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.ares.shop.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Chấm điểm rủi ro gian lận cho đơn hàng (quy tắc, có giải thích). ≥ REVIEW_AT → đưa vào hàng chờ duyệt và
 * chặn xác nhận/giao cho tới khi quản trị duyệt; ≥ WATCH_AT → đánh dấu theo dõi.
 */
@Service
public class FraudService {
    public static final int REVIEW_AT = 60, WATCH_AT = 30;
    static final Set<String> DISPOSABLE = Set.of("mailinator.com", "yopmail.com", "guerrillamail.com", "10minutemail.com", "tempmail.com", "temp-mail.org", "trashmail.com", "sharklasers.com", "getnada.com", "dispostable.com");
    final OrderRepo orders; final PaymentTxnRepo txns;
    public FraudService(OrderRepo orders, PaymentTxnRepo txns) { this.orders = orders; this.txns = txns; }

    public record Signal(String code, int points, String reason) {}

    static String ip() {
        try {
            var a = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes(); if (a == null) return null;
            var rq = a.getRequest(); String x = rq.getHeader("X-Forwarded-For");
            return x != null && !x.isBlank() ? x.split(",")[0].trim() : rq.getRemoteAddr();
        } catch (Exception e) { return null; }
    }
    static String phone(String p) { if (p == null) return ""; String d = p.replaceAll("\\D", ""); return d.startsWith("84") ? "0" + d.substring(2) : d; }

    public List<Signal> signals(CustomerOrder o) {
        List<Signal> s = new ArrayList<>(); LocalDateTime now = o.createdAt == null ? LocalDateTime.now() : o.createdAt;
        List<CustomerOrder> all = orders.findAll(); String ph = phone(o.phone);
        boolean guest = o.user == null;
        if (guest) s.add(new Signal("GUEST", 10, "Khách vãng lai (không tài khoản)"));
        else if (o.user.createdAt != null && o.user.createdAt.isAfter(now.minusHours(24))) s.add(new Signal("NEW_ACCOUNT", 15, "Tài khoản mới tạo < 24 giờ"));
        if (o.total >= 15_000_000) s.add(new Signal("VERY_HIGH_VALUE", 30, "Giá trị đơn rất cao (≥ 15 triệu)"));
        else if (o.total >= 5_000_000) s.add(new Signal("HIGH_VALUE", 15, "Giá trị đơn cao (≥ 5 triệu)"));
        if (o.paymentMethod == Enums.PaymentMethod.COD && o.total >= 5_000_000) s.add(new Signal("COD_HIGH", 15, "COD giá trị lớn — rủi ro bom hàng"));
        if (!guest) {
            var prev = all.stream().filter(x -> x.user != null && x.user.id.equals(o.user.id) && !x.id.equals(o.id)).toList();
            var done = prev.stream().filter(x -> x.status == Enums.OrderStatus.COMPLETED).mapToLong(x -> x.total).toArray();
            if (done.length >= 2) { double avg = Arrays.stream(done).average().orElse(0); if (o.total > avg * 3 && o.total - avg > 2_000_000) s.add(new Signal("SPIKE", 15, "Gấp > 3 lần giá trị đơn trung bình của khách")); }
            long bad = prev.stream().filter(x -> x.status == Enums.OrderStatus.CANCELLED || x.status == Enums.OrderStatus.RETURNED).count();
            if (prev.size() >= 2 && bad * 2 >= prev.size()) s.add(new Signal("BAD_HISTORY", 20, "≥ 50% đơn trước bị huỷ/hoàn (" + bad + "/" + prev.size() + ")"));
        }
        if (!ph.isEmpty()) {
            long sameDay = all.stream().filter(x -> !x.id.equals(o.id) && ph.equals(phone(x.phone)) && x.createdAt != null && x.createdAt.isAfter(now.minusHours(24))).count();
            if (sameDay >= 2) s.add(new Signal("PHONE_VELOCITY", 20, "Cùng SĐT đặt " + (sameDay + 1) + " đơn trong 24 giờ"));
            long ids = all.stream().filter(x -> ph.equals(phone(x.phone)) && x.email != null).map(x -> x.email.toLowerCase()).distinct().count();
            if (ids >= 3) s.add(new Signal("PHONE_SHARED", 15, "SĐT dùng với " + ids + " email khác nhau"));
        }
        if (!ph.matches("0[35789]\\d{8}")) s.add(new Signal("BAD_PHONE", 15, "SĐT không đúng định dạng di động VN"));
        if (o.clientIp != null) {
            long ipHour = all.stream().filter(x -> !x.id.equals(o.id) && o.clientIp.equals(x.clientIp) && x.createdAt != null && x.createdAt.isAfter(now.minusHours(1))).count();
            if (ipHour >= 3) s.add(new Signal("IP_VELOCITY", 20, "Cùng IP đặt " + (ipHour + 1) + " đơn trong 1 giờ"));
        }
        if (o.email != null && o.email.contains("@") && DISPOSABLE.contains(o.email.substring(o.email.indexOf('@') + 1).toLowerCase())) s.add(new Signal("DISPOSABLE_EMAIL", 20, "Email dùng một lần"));
        int maxQty = o.items.stream().mapToInt(i -> i.quantity).max().orElse(0);
        if (maxQty >= 5) s.add(new Signal("BULK_QTY", 15, "Mua " + maxQty + " cái cùng một mẫu"));
        if (o.shippingAddress == null || o.shippingAddress.trim().length() < 15) s.add(new Signal("SHORT_ADDRESS", 10, "Địa chỉ giao quá ngắn/thiếu"));
        if (o.buyerName == null || o.buyerName.trim().split("\\s+").length < 2) s.add(new Signal("SHORT_NAME", 5, "Tên người nhận không đầy đủ"));
        long fails = txns.findByOrderCodeOrderByIdDesc(o.code == null ? "" : o.code).stream().filter(t -> "FAILED".equals(t.status)).count();
        if (fails >= 3) s.add(new Signal("PAY_FAILS", 15, fails + " lần thanh toán thất bại"));
        if (o.discount > 0 && o.subtotal > 0 && o.discount * 100 / o.subtotal >= 40) s.add(new Signal("DEEP_DISCOUNT", 10, "Giảm giá ≥ 40% giá trị đơn"));
        return s;
    }

    public CustomerOrder assess(CustomerOrder o) {
        if (o.clientIp == null) o.clientIp = ip();
        List<Signal> s = signals(o);
        int score = Math.min(100, s.stream().mapToInt(Signal::points).sum());
        o.fraudScore = score; o.fraudReasons = String.join("; ", s.stream().map(x -> x.code() + ":" + x.points() + ":" + x.reason()).toList());
        if (!"APPROVED".equals(o.fraudStatus) && !"REJECTED".equals(o.fraudStatus)) o.fraudStatus = score >= REVIEW_AT ? "REVIEW" : score >= WATCH_AT ? "WATCH" : "NONE";
        return orders.save(o);
    }

    /** Chặn xác nhận / giao hàng khi đơn đang chờ duyệt gian lận (enforce phía server). */
    public static void requireCleared(CustomerOrder o, Enums.OrderStatus target) {
        if ("REVIEW".equals(o.fraudStatus) && (target == Enums.OrderStatus.CONFIRMED || target == Enums.OrderStatus.SHIPPING || target == Enums.OrderStatus.COMPLETED))
            throw ApiException.bad("Đơn " + o.code + " đang chờ duyệt rủi ro gian lận (điểm " + o.fraudScore + ") — duyệt trong mục Gian lận trước");
    }
}
