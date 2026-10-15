package com.ares.shop.service;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.function.Supplier;

/** Tác vụ nền định kỳ (@Scheduled). Mỗi lần chạy ghi JobRun; admin xem & chạy tay ở /admin/jobs. */
@Service
public class JobService {
    private final AbandonedCartRepo carts; private final ProductRepo products; private final PreOrderRepo preorders; private final PromotionRepo promos;
    private final OrderRepo orders; private final UserRepo users; private final MailService mail; private final NotifyService notify; private final JobRunRepo runs;
    public JobService(AbandonedCartRepo carts, ProductRepo products, PreOrderRepo preorders, PromotionRepo promos, OrderRepo orders, UserRepo users,
                      MailService mail, NotifyService notify, JobRunRepo runs) {
        this.carts = carts; this.products = products; this.preorders = preorders; this.promos = promos; this.orders = orders; this.users = users;
        this.mail = mail; this.notify = notify; this.runs = runs;
    }
    public static final Map<String, String> JOBS = new LinkedHashMap<>();
    static {
        JOBS.put("cart-reminder", "Nhắc giỏ hàng bị bỏ quên (> 2 giờ, tối đa 1 lần/24h) — mỗi 15 phút");
        JOBS.put("preorder-release", "Báo hàng về cho khách đặt trước khi mẫu đã phát hành và có tồn — mỗi 10 phút");
        JOBS.put("promo-expire", "Tắt voucher đã hết hạn hoặc hết lượt — mỗi giờ");
        JOBS.put("daily-digest", "Email tổng kết doanh số hôm qua cho admin — 08:00 hằng ngày");
        JOBS.put("review-request", "Mời đánh giá đơn đã giao 3 ngày — 09:00 hằng ngày");
    }

    private String track(String job, String trigger, Supplier<String> work) {
        JobRun r = new JobRun(); r.job = job; r.trigger = trigger; long t = System.currentTimeMillis();
        try { r.result = work.get(); r.ok = true; } catch (Exception e) { r.ok = false; r.result = "Lỗi: " + e.getMessage(); }
        r.ms = System.currentTimeMillis() - t; if (r.result != null && r.result.length() > 1000) r.result = r.result.substring(0, 1000); runs.save(r); return r.result;
    }
    @Transactional public String run(String job, String trigger) {
        return switch (job) {
            case "cart-reminder" -> track(job, trigger, this::cartReminder);
            case "preorder-release" -> track(job, trigger, this::preorderRelease);
            case "promo-expire" -> track(job, trigger, this::promoExpire);
            case "daily-digest" -> track(job, trigger, this::dailyDigest);
            case "review-request" -> track(job, trigger, this::reviewRequest);
            default -> throw new IllegalArgumentException("Không có tác vụ " + job);
        };
    }
    @Scheduled(fixedDelay = 15 * 60_000, initialDelay = 90_000) public void s1() { run("cart-reminder", "auto"); }
    @Scheduled(fixedDelay = 10 * 60_000, initialDelay = 60_000) public void s2() { run("preorder-release", "auto"); }
    @Scheduled(cron = "0 5 * * * *") public void s3() { run("promo-expire", "auto"); }
    @Scheduled(cron = "0 0 8 * * *") public void s4() { run("daily-digest", "auto"); }
    @Scheduled(cron = "0 0 9 * * *") public void s5() { run("review-request", "auto"); }

    String cartReminder() {
        var now = LocalDateTime.now(); int n = 0;
        for (AbandonedCart c : carts.findAll()) {
            if (c.email == null || c.subtotal <= 0 || c.lastSeen.isAfter(now.minusHours(2))) continue;
            if (c.remindedAt != null && c.remindedAt.isAfter(now.minusHours(24))) continue;
            mail.send(c.email, "Bạn còn quên vài món trong giỏ ARES Toys 🏁", "CART_REMINDER", "Giỏ hàng trị giá " + MailService.vnd(c.subtotal) + " vẫn đang chờ bạn. Dùng mã COMEBACK5 giảm 5% trong 24h.\nTiếp tục: /cart");
            c.remindedAt = now; carts.save(c); n++;
        }
        return "Đã nhắc " + n + " giỏ";
    }
    String preorderRelease() {
        int n = 0; var today = LocalDate.now();
        for (Product p : products.findAll()) {
            if (!Boolean.TRUE.equals(p.preorder) || p.releaseDate == null || p.releaseDate.isAfter(today) || p.stock <= 0) continue;
            for (PreOrder o : preorders.findByProductIdAndStatus(p.id, "WAITING")) {
                o.status = "NOTIFIED"; o.notifiedAt = LocalDateTime.now(); preorders.save(o); n++;
                mail.send(o.email, "Hàng về: " + p.name, "PREORDER", "Mẫu " + p.name + " bạn đặt trước đã về kho. Hoàn tất đơn trong 72 giờ: /products/" + p.id);
                if (o.userId != null) users.findById(o.userId).ifPresent(u -> notify.send(u, "SYSTEM", "Hàng đặt trước đã về", p.name + " đã sẵn sàng.", "/products/" + p.id));
            }
        }
        return "Đã báo " + n + " khách";
    }
    String promoExpire() {
        int n = 0; var today = LocalDate.now();
        for (Promotion p : promos.findByActiveTrue()) {
            boolean expired = p.endDate != null && p.endDate.isBefore(today), used = p.usageLimit > 0 && p.usedCount >= p.usageLimit;
            if (expired || used) { p.active = false; promos.save(p); n++; }
        }
        return "Đã tắt " + n + " voucher";
    }
    String dailyDigest() {
        var y = LocalDate.now().minusDays(1);
        var list = orders.findAll().stream().filter(o -> o.createdAt.toLocalDate().equals(y) && o.status != Enums.OrderStatus.CANCELLED).toList();
        long rev = list.stream().mapToLong(o -> o.total).sum();
        String body = "Tổng kết " + y + ":\n• Đơn hàng: " + list.size() + "\n• Doanh thu: " + MailService.vnd(rev) + "\n• Sắp hết hàng: " + products.findByActiveTrueAndStockLessThanEqualOrderByStockAsc(5).size() + " sản phẩm";
        long sent = users.findAll().stream().filter(u -> u.role == Enums.Role.ADMIN).peek(u -> mail.send(u.email, "Tổng kết ngày " + y + " — ARES Toys", "DIGEST", body)).count();
        return "Gửi " + sent + " admin · " + list.size() + " đơn · " + MailService.vnd(rev);
    }
    String reviewRequest() {
        int n = 0; var day = LocalDate.now().minusDays(3);
        for (CustomerOrder o : orders.findAll()) {
            var done = o.timeline.get("COMPLETED");
            if (o.status != Enums.OrderStatus.COMPLETED || done == null || !done.toLocalDate().equals(day) || o.email == null) continue;
            mail.send(o.email, "Bạn thấy đơn " + o.code + " thế nào?", "REVIEW_REQUEST", "Cảm ơn bạn đã mua sắm! Đánh giá sản phẩm để nhận điểm thưởng: /orders/" + o.code); n++;
        }
        return "Đã mời " + n + " đơn";
    }
    public List<JobRun> recent() { return runs.findTop100ByOrderByIdDesc(); }
}
