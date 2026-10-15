package com.ares.shop.service;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

/** Quét định kỳ: báo giảm giá / có hàng lại cho khách, cảnh báo tồn thấp cho nhân viên. */
@Service
public class AlertService {
    public static final int LOW_STOCK = 5;
    private final AlertRepo alerts; private final ProductRepo products; private final UserRepo users; private final NotifyService notify;
    private volatile boolean primed;
    private final Set<Long> lowNotified = Collections.synchronizedSet(new HashSet<>());

    public AlertService(AlertRepo alerts, ProductRepo products, UserRepo users, NotifyService notify) {
        this.alerts = alerts; this.products = products; this.users = users; this.notify = notify;
    }

    @org.springframework.beans.factory.annotation.Autowired private com.ares.shop.service.MailService mail;
    @org.springframework.beans.factory.annotation.Autowired private com.ares.shop.repo.StockNotifyRepo stockNotify;

    @Scheduled(fixedDelay = 30_000, initialDelay = 20_000)
    public void scan() {
        for (StockNotify n : stockNotify.findBySentIsNullOrSentFalse()) {
            Product p = products.findById(n.productId).orElse(null);
            if (p == null || !p.active || p.stock <= 0) continue;
            mail.send(n.email, "Đã có hàng: " + p.name, "BACK_IN_STOCK", "Sản phẩm bạn quan tâm đã có hàng trở lại (còn " + p.stock + ").\nĐặt ngay: /products/" + p.id);
            n.sent = true; stockNotify.save(n);
        }
        for (WatchAlert w : alerts.findByFiredIsNullOrFiredFalse()) {
            Product p = products.findById(w.productId).orElse(null);
            if (p == null || !p.active) continue;
            boolean hit = "PRICE".equals(w.kind) ? p.price < w.basePrice : p.stock > 0;
            if (!hit) continue;
            String title = "PRICE".equals(w.kind) ? "Giảm giá: " + p.name : "Đã có hàng lại: " + p.name;
            String body = "PRICE".equals(w.kind) ? "Giá mới " + String.format("%,d", p.price).replace(',', '.') + "đ (trước " + String.format("%,d", w.basePrice).replace(',', '.') + "đ)." : "Còn " + p.stock + " sản phẩm — đặt ngay kẻo hết.";
            notify.send(w.user, "SYSTEM", title, body, "/products/" + p.id);
            w.fired = true; alerts.save(w);
        }
        var staff = users.findAll().stream().filter(u -> u.role != Enums.Role.USER && !u.locked).toList();
        var low = products.findByActiveTrueAndStockLessThanEqualOrderByStockAsc(LOW_STOCK);
        if (!primed) { low.forEach(p -> lowNotified.add(p.id)); primed = true; return; } // lần đầu: chỉ ghi nhận, chỉ báo khi chuyển sang tồn thấp
        for (Product p : low) {
            if (!lowNotified.add(p.id)) continue;
            staff.forEach(s -> notify.send(s, "SYSTEM", "Tồn kho thấp: " + p.name, "Chỉ còn " + p.stock + " sản phẩm (SKU " + p.sku + ").", "/admin/inventory"));
            staff.stream().filter(s -> s.role == Enums.Role.ADMIN).forEach(s -> mail.send(s.email, "[Tồn kho thấp] " + p.name, "LOW_STOCK",
                    "Sản phẩm " + p.name + " (SKU " + p.sku + ") chỉ còn " + p.stock + " sản phẩm.\nVào /admin/purchase-orders để tạo đơn nhập hàng theo gợi ý."));
        }
        // sản phẩm đã nhập thêm hàng → cho phép cảnh báo lại lần sau
        lowNotified.removeIf(id -> products.findById(id).map(p -> p.stock > LOW_STOCK).orElse(true));
    }
}
