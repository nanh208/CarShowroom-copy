package com.ares.shop.config;

import com.ares.shop.model.Post;
import com.ares.shop.repo.PostRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.*;

/** Bài viết mẫu cho CMS (chỉ khi chưa có bài nào). */
@Component
public class V16Seeder implements CommandLineRunner {
    private final PostRepo posts;
    public V16Seeder(PostRepo posts) { this.posts = posts; }
    @Override public void run(String... a) {
        if (posts.count() > 0) return;
        add("chon-ti-le-xe-mo-hinh-1-64-1-24-1-18", "Chọn tỉ lệ xe mô hình: 1:64, 1:24 hay 1:18?", "How to pick a scale: 1:64, 1:24 or 1:18?", "BLOG", null, "/images/toys/bburago-chiron-0.jpg",
            "Hướng dẫn nhanh để chọn tỉ lệ phù hợp với ngân sách, không gian trưng bày và độ chi tiết mong muốn.",
            "<h2>1:64 — nhỏ gọn, dễ sưu tầm</h2><p>Khoảng <strong>7 cm</strong>, giá mềm, phù hợp sưu tầm số lượng lớn. Hot Wheels, Mini GT, Tomica là các dòng phổ biến.</p><h2>1:24 — cân bằng</h2><p>Khoảng 19 cm, mở được cửa/capo, chi tiết nội thất rõ.</p><h2>1:18 — tủ trưng bày</h2><p>Khoảng 25 cm, chi tiết cao nhất, hợp làm điểm nhấn.</p><blockquote>Mẹo: chọn một tỉ lệ chủ đạo để bộ sưu tập đồng đều.</blockquote>",
            List.of("tỉ lệ", "hướng dẫn", "sưu tầm"), List.of(1L, 10L), 6);
        add("bao-quan-pin-lipo-an-toan", "Bảo quản pin LiPo an toàn cho xe RC", "Storing LiPo batteries safely", "BLOG", null, null,
            "5 nguyên tắc để pin LiPo bền và an toàn: sạc cân bằng, điện áp lưu kho 3.8V/cell, túi chống cháy…",
            "<ol><li>Luôn dùng <strong>sạc cân bằng</strong>.</li><li>Không sạc qua đêm khi không có người giám sát.</li><li>Lưu kho ở 3.8V/cell.</li><li>Cất trong túi chống cháy.</li><li>Ngưng dùng khi pin phồng.</li></ol>",
            List.of("xe rc", "pin lipo", "hướng dẫn"), List.of(), 4);
        add("hang-moi-ve-thang-10", "Hàng mới về tháng 10: Mini GT, Tamiya, LEGO Technic", "New arrivals in October", "NEWS", null, null,
            "Loạt mẫu mới cập bến ARES Toys — số lượng có hạn, đặt trước để giữ hàng.",
            "<p>Tháng này ARES Toys nhập thêm nhiều mẫu <strong>Mini GT</strong>, bộ <strong>Tamiya</strong> kỷ niệm và LEGO Technic. Xem ngay ở trang sản phẩm mới.</p>",
            List.of("hàng mới", "tin tức"), List.of(), 2);
        add("review-traxxas-maxx-video", "Video review: Traxxas Maxx chạy thử địa hình", "Video review: Traxxas Maxx off-road test", "VIDEO", "https://www.youtube-nocookie.com/embed/ysz5S6PUM-U", null,
            "Chạy thử Traxxas Maxx trên đồi cát và bãi sỏi: tốc độ, độ bền và thời lượng pin.",
            "<p>Trong video, chúng tôi kiểm tra khả năng vượt địa hình, tốc độ tối đa và thời lượng pin của Traxxas Maxx.</p>",
            List.of("xe rc", "review", "video"), List.of(), 1);
    }
    void add(String slug, String title, String titleEn, String kind, String video, String cover, String excerpt, String html, List<String> tags, List<Long> pids, int daysAgo) {
        Post p = new Post(); p.slug = slug; p.title = title; p.titleEn = titleEn; p.kind = kind; p.videoUrl = video; p.coverImage = cover; p.excerpt = excerpt; p.contentHtml = html;
        p.tags = new LinkedHashSet<>(tags); p.productIds = new ArrayList<>(pids); p.status = "PUBLISHED"; p.publishedAt = LocalDateTime.now().minusDays(daysAgo); p.author = "ARES Toys"; posts.save(p);
    }
}
