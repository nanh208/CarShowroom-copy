package com.ares.shop.service;
import com.ares.shop.model.*; import com.ares.shop.repo.*; import org.springframework.stereotype.Service;
/**
 * Gửi email qua Spring Mail. Mọi email luôn được lưu vào hộp thư đi (EmailLog) để admin xem.
 * Chỉ gửi SMTP thật khi cấu hình spring.mail.host (mặc định: chế độ mô phỏng, không gửi ra ngoài).
 */
@Service
public class MailService {
    private final EmailLogRepo logs; private final org.springframework.beans.factory.ObjectProvider<org.springframework.mail.javamail.JavaMailSender> sender;
    @org.springframework.beans.factory.annotation.Value("${spring.mail.host:}") String host;
    @org.springframework.beans.factory.annotation.Value("${ares.mail.from:no-reply@ares.vn}") String from;
    public MailService(EmailLogRepo logs, org.springframework.beans.factory.ObjectProvider<org.springframework.mail.javamail.JavaMailSender> sender) { this.logs = logs; this.sender = sender; }
    public EmailLog send(String to, String subject, String kind, String body) {
        if (to == null || to.isBlank()) return null;
        EmailLog e = new EmailLog(); e.toAddr = to.trim(); e.subject = subject; e.kind = kind; e.body = body; logs.save(e);
        var s = host == null || host.isBlank() ? null : sender.getIfAvailable();
        if (s != null) {
            try { var m = new org.springframework.mail.SimpleMailMessage(); m.setFrom(from); m.setTo(e.toAddr); m.setSubject(subject); m.setText(body); s.send(m); }
            catch (Exception ex) { org.slf4j.LoggerFactory.getLogger(MailService.class).warn("SMTP lỗi, chỉ lưu outbox: {}", ex.getMessage()); }
        }
        return e;
    }
    public static String vnd(long v) { return String.format("%,d", v).replace(',', '.') + "đ"; }
    public EmailLog invoice(CustomerOrder o) {
        StringBuilder b = new StringBuilder();
        b.append("Xin chào ").append(o.buyerName).append(",\n\nCảm ơn bạn đã mua sắm tại ARES Toys. Hoá đơn đơn hàng ").append(o.code).append(":\n\n");
        for (OrderItem i : o.items) b.append("• ").append(i.productName).append(i.variantName == null ? "" : " – " + i.variantName)
                .append("  x").append(i.quantity).append("  ").append(vnd(i.unitPrice * i.quantity)).append('\n');
        b.append("\nTạm tính: ").append(vnd(o.subtotal));
        if (o.discount > 0) b.append("\nGiảm giá: -").append(vnd(o.discount));
        if (o.pointsDiscount > 0) b.append("\nĐiểm thưởng: -").append(vnd(o.pointsDiscount));
        b.append("\nPhí vận chuyển (").append("EXPRESS".equals(o.shippingMethod) ? "hoả tốc" : "tiêu chuẩn").append("): ").append(vnd(o.shippingFee));
        if (o.giftFee != null) b.append("\nGói quà: ").append(vnd(o.giftFee));
        if (o.creditUsed != null) b.append("\nThẻ quà / tín dụng: -").append(vnd(o.creditUsed));
        b.append("\nTỔNG CỘNG: ").append(vnd(o.total));
        if (o.etaFrom != null) b.append("\n\nDự kiến giao: ").append(o.etaFrom.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"))).append(" – ").append(o.etaTo.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        b.append("\nGiao tới: ").append(o.shippingAddress).append("\nTheo dõi đơn: /track/").append(o.code).append("\n\nARES Toys — Nhỏ tỉ lệ, lớn đam mê.");
        return send(o.email, "Hoá đơn đơn hàng " + o.code + " — ARES Toys", "INVOICE", b.toString());
    }
}
