package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Đánh giá sản phẩm, thông báo, đăng ký nhận tin, thống kê doanh thu. */
@RestController @RequestMapping("/api")
public class ExtrasController {
    private final ReviewRepo reviews; private final NotificationRepo notes; private final SubscriberRepo subs;
    private final ProductRepo products; private final OrderRepo orders;

    public ExtrasController(ReviewRepo reviews, NotificationRepo notes, SubscriberRepo subs, ProductRepo products, OrderRepo orders) {
        this.reviews = reviews; this.notes = notes; this.subs = subs; this.products = products; this.orders = orders;
    }

    // ===== Đánh giá =====
    @GetMapping("/products/{id}/reviews")
    public Map<String, Object> reviews(@PathVariable Long id) {
        var list = reviews.findByProductIdAndApprovedTrueOrderByCreatedAtDesc(id);
        double avg = list.stream().mapToInt(r -> r.rating).average().orElse(0);
        int[] dist = new int[5];
        list.forEach(r -> dist[Math.max(1, Math.min(5, r.rating)) - 1]++);
        User u = Current.userOrNull();
        return Map.of("items", list, "avg", Math.round(avg * 10) / 10.0, "count", list.size(), "dist", dist,
                "mine", u != null && reviews.existsByProductIdAndUserId(id, u.id));
    }

    /** Đánh giá mới nhất (tên khách được che bớt) cho trang chủ. */
    @GetMapping("/products/reviews/latest")
    public List<Map<String, Object>> latest(@RequestParam(defaultValue = "3") int limit) {
        return reviews.findAllByOrderByCreatedAtDesc().stream().filter(r -> r.approved && r.rating >= 4)
                .collect(Collectors.toMap(r -> r.productId, r -> r, (a, b) -> a, LinkedHashMap::new)).values().stream()
                .limit(Math.min(limit, 12)).map(r -> Map.<String, Object>of("id", r.id, "productId", r.productId, "productName", r.productName,
                        "author", com.ares.shop.service.AuctionService.mask(r.authorName), "rating", r.rating, "comment", r.comment, "createdAt", r.createdAt)).toList();
    }

    @GetMapping("/products/ratings")
    public Map<Long, double[]> ratings() {
        return reviews.findByApprovedTrue().stream().collect(Collectors.groupingBy(r -> r.productId)).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new double[]{
                        Math.round(e.getValue().stream().mapToInt(r -> r.rating).average().orElse(0) * 10) / 10.0, e.getValue().size()}));
    }

    public record ReviewReq(int rating, String comment) {}

    @PostMapping("/products/{id}/reviews")
    public Review addReview(@PathVariable Long id, @RequestBody ReviewReq r) {
        User u = Current.user();
        Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm"));
        if (r.rating() < 1 || r.rating() > 5) throw ApiException.bad("Vui lòng chọn số sao từ 1 đến 5");
        if (r.comment() == null || r.comment().trim().length() < 5) throw ApiException.bad("Nội dung đánh giá tối thiểu 5 ký tự");
        if (reviews.existsByProductIdAndUserId(id, u.id)) throw ApiException.bad("Bạn đã đánh giá sản phẩm này");
        Review x = new Review(); x.productId = id; x.productName = p.name; x.user = u; x.authorName = u.fullName;
        x.rating = r.rating(); x.comment = r.comment().trim();
        return reviews.save(x);
    }

    @GetMapping("/admin/reviews")
    public List<Review> allReviews() { return reviews.findAllByOrderByCreatedAtDesc(); }

    @PatchMapping("/admin/reviews/{id}")
    public Review moderate(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Review r = reviews.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy đánh giá"));
        r.approved = Boolean.TRUE.equals(body.get("approved"));
        return reviews.save(r);
    }

    @DeleteMapping("/admin/reviews/{id}")
    public void deleteReview(@PathVariable Long id) { reviews.deleteById(id); }

    // ===== Thông báo =====
    @GetMapping("/account/notifications")
    public Map<String, Object> notifications() {
        User u = Current.user();
        return Map.of("items", notes.findTop30ByUserIdOrderByCreatedAtDesc(u.id), "unread", notes.findByUserIdAndReadFalse(u.id).size());
    }

    @Transactional @PostMapping("/account/notifications/read")
    public void readAll() {
        User u = Current.user();
        var list = notes.findByUserIdAndReadFalse(u.id); list.forEach(n -> n.read = true); notes.saveAll(list);
    }

    // ===== Nhận tin =====
    @PostMapping("/newsletter")
    public Map<String, String> subscribe(@RequestBody Map<String, String> body) {
        String email = Optional.ofNullable(body.get("email")).orElse("").trim();
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw ApiException.bad("Email không hợp lệ");
        if (!subs.existsByEmailIgnoreCase(email)) { Subscriber s = new Subscriber(); s.email = email; subs.save(s); }
        return Map.of("message", "Đã đăng ký nhận tin. Cảm ơn bạn!");
    }

    // ===== Doanh thu 14 ngày =====
    @GetMapping("/admin/revenue")
    public List<Map<String, Object>> revenue(@RequestParam(defaultValue = "14") int days) {
        var all = orders.findAll();
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            long sum = all.stream().filter(o -> o.status != Enums.OrderStatus.CANCELLED && o.createdAt.toLocalDate().equals(d)).mapToLong(o -> o.paidAmount).sum();
            long cnt = all.stream().filter(o -> o.createdAt.toLocalDate().equals(d)).count();
            out.add(Map.of("date", d.toString(), "revenue", sum, "orders", cnt));
        }
        return out;
    }
}
