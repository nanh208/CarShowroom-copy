package com.ares.shop.service;

import com.ares.shop.model.CustomerOrder;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Sự kiện miền đơn hàng: phát từ service/controller, xử lý tách rời (email trạng thái…). */
@Component
public class OrderEvents {
    public record OrderPlaced(CustomerOrder order) {}
    public record OrderStatusChanged(CustomerOrder order, String from, String to) {}
    private final MailService mail;
    public OrderEvents(MailService mail) { this.mail = mail; }

    @EventListener
    public void onStatus(OrderStatusChanged e) {
        var o = e.order(); if (o.email == null) return;
        String msg = switch (e.to()) {
            case "CONFIRMED" -> "Đơn " + o.code + " đã được xác nhận và đang đóng gói.";
            case "SHIPPING" -> "Đơn " + o.code + " đã giao cho đơn vị vận chuyển" + (o.etaTo == null ? "." : ", dự kiến tới trước " + o.etaTo + ".");
            case "COMPLETED" -> "Đơn " + o.code + " đã giao thành công. Cảm ơn bạn! Hãy để lại đánh giá để nhận điểm thưởng: /orders/" + o.code;
            case "CANCELLED" -> "Đơn " + o.code + " đã huỷ" + (o.cancelReason == null ? "." : ": " + o.cancelReason + ".");
            default -> null;
        };
        if (msg != null) mail.send(o.email, "Cập nhật đơn " + o.code, "ORDER_STATUS", msg + "\nTheo dõi: /track/" + o.code);
    }
}
