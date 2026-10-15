package com.ares.shop.service;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

/** Mua định kỳ: đến hạn thì tự tạo đơn (giảm discountPct%), lỗi 3 lần liên tiếp → tạm dừng. */
@Service
public class SubscriptionService {
    public static final int MAX_FAILS = 3;
    private final SubscriptionRepo subs; private final OrderService orders; private final OrderRepo orderRepo; private final NotifyService notify; private final UserRepo users;
    public SubscriptionService(SubscriptionRepo subs, OrderService orders, OrderRepo orderRepo, NotifyService notify, UserRepo users) {
        this.subs = subs; this.orders = orders; this.orderRepo = orderRepo; this.notify = notify; this.users = users;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 40_000)
    public void tick() { runDue(); }

    /** Chạy mọi đăng ký đến hạn. Trả về số đơn tạo thành công. */
    public synchronized Map<String, Object> runDue() {
        int ok = 0, fail = 0;
        for (ProductSubscription s : subs.findByStatusAndNextRunAtBefore("ACTIVE", LocalDateTime.now().plusSeconds(1))) { if (runOne(s) != null) ok++; else fail++; }
        return Map.of("created", ok, "failed", fail);
    }

    /** Tạo 1 đơn cho đăng ký; lỗi (hết hàng, sản phẩm ngừng bán…) được ghi lại, không ném ra ngoài. */
    public CustomerOrder runOne(ProductSubscription s) {
        User u = s.user == null ? null : users.findById(s.user.id).orElse(null);
        if (u == null || u.locked) { s.status = "PAUSED"; s.lastError = "Tài khoản không khả dụng"; subs.save(s); return null; }
        try {
            var req = new OrderService.CheckoutReq(s.buyerName, s.phone, u.email, s.shippingAddress, s.city, "Đơn định kỳ #" + s.id + " (mỗi " + s.intervalDays + " ngày)",
                null, null, s.paymentMethod, null, List.of(new OrderService.ItemReq(s.productId, s.variantId, s.quantity)),
                null, null, "STANDARD", null, null, false, false, false, null);
            CustomerOrder o = orders.place(u, req);
            long d = Math.round(o.subtotal * s.discountPct / 100.0);
            if (d > 0 && o.total - d >= 0 && o.paymentStatus != Enums.PaymentStatus.PAID) { o.discount += d; o.total -= d; o = orderRepo.save(o); }
            s.runs++; s.failCount = 0; s.lastError = null; s.lastOrderCode = o.code; s.lastRunAt = LocalDateTime.now();
            s.nextRunAt = LocalDateTime.now().plusDays(s.intervalDays); subs.save(s);
            notify.send(u, "ORDER", "Đơn định kỳ đã tạo", "Đơn " + o.code + " cho \"" + s.productName + "\" (giảm " + s.discountPct + "%). Lần tiếp theo: " + s.nextRunAt.toLocalDate() + ".", "/orders/" + o.code);
            return o;
        } catch (Exception e) {
            s.failCount++; s.lastError = e.getMessage(); s.lastRunAt = LocalDateTime.now();
            if (s.failCount >= MAX_FAILS) s.status = "PAUSED"; else s.nextRunAt = LocalDateTime.now().plusDays(1);
            subs.save(s);
            notify.send(u, "SYSTEM", "Không tạo được đơn định kỳ", "\"" + s.productName + "\": " + e.getMessage() + (s.status.equals("PAUSED") ? " — đã tạm dừng đăng ký." : " — sẽ thử lại sau 1 ngày."), "/account/subscriptions");
            return null;
        }
    }
}
