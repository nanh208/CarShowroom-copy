package com.ares.shop.service;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

/** Tiếp thị liên kết: ghi nhận hoa hồng khi đặt đơn qua mã, đồng bộ trạng thái theo đơn. */
@Service
public class AffiliateService {
    private final AffiliateRepo affs; private final CommissionRepo coms; private final OrderRepo orders; private final NotifyService notify;
    public AffiliateService(AffiliateRepo affs, CommissionRepo coms, OrderRepo orders, NotifyService notify) { this.affs = affs; this.coms = coms; this.orders = orders; this.notify = notify; }

    /** Gắn đơn với đối tác (bỏ qua: mã sai, đối tác bị khoá, tự giới thiệu chính mình). Trả về hoa hồng hoặc null. */
    public AffiliateCommission attribute(CustomerOrder o, String code) {
        if (o == null || code == null || code.isBlank()) return null;
        Affiliate a = affs.findByCodeIgnoreCase(code.trim()).orElse(null);
        if (a == null || !"ACTIVE".equals(a.status)) return null;
        if (o.user != null && a.user != null && a.user.id.equals(o.user.id)) return null;
        if (a.user != null && o.email != null && o.email.equalsIgnoreCase(a.user.email)) return null;
        if (coms.findByOrderCode(o.code).isPresent()) return null;
        AffiliateCommission c = new AffiliateCommission();
        c.affiliateId = a.id; c.affiliateCode = a.code; c.orderCode = o.code; c.pct = a.commissionPct;
        c.base = Math.max(0, o.subtotal - o.discount - o.pointsDiscount); c.amount = Math.round(c.base * a.commissionPct / 100.0);
        coms.save(c);
        notify.send(a.user, "SYSTEM", "Có đơn mới qua liên kết của bạn", "Đơn " + o.code + " — hoa hồng tạm tính " + String.format("%,d", c.amount).replace(',', '.') + "đ (chờ giao thành công).", "/account/affiliate");
        return c;
    }

    /** PENDING → APPROVED khi đơn COMPLETED; → VOID khi CANCELLED/RETURNED. */
    @Scheduled(fixedDelay = 60_000, initialDelay = 45_000)
    public void sync() {
        for (String st : new String[]{"PENDING", "APPROVED"}) for (AffiliateCommission c : coms.findByStatus(st)) {
            var o = orders.findByCode(c.orderCode).orElse(null);
            if (o == null) continue;
            if (o.status == Enums.OrderStatus.CANCELLED || o.status == Enums.OrderStatus.RETURNED) { c.status = "VOID"; coms.save(c); }
            else if ("PENDING".equals(st) && o.status == Enums.OrderStatus.COMPLETED) { c.status = "APPROVED"; c.approvedAt = LocalDateTime.now(); coms.save(c); }
        }
    }
}
