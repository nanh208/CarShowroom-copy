package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.ares.shop.service.JobService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/** Tuỳ chọn cá nhân lưu server, sản phẩm đã xem, và quản lý tác vụ nền. */
@RestController
public class PlatformController {
    private final UserPrefRepo prefs; private final ProductViewRepo views; private final ProductRepo products; private final JobService jobs;
    private final ObjectMapper json = new ObjectMapper();
    public PlatformController(UserPrefRepo prefs, ProductViewRepo views, ProductRepo products, JobService jobs) { this.prefs = prefs; this.views = views; this.products = products; this.jobs = jobs; }

    /** Khoá cho phép + kiểm tra định dạng. */
    static final Map<String, Integer> KEYS = Map.of("compare", 3, "dashWidgets", 0, "wishlist", 50, "locale", -1, "currency", -2);

    @GetMapping("/api/account/prefs")
    public Map<String, Object> allPrefs() {
        Map<String, Object> m = new LinkedHashMap<>();
        for (UserPref p : prefs.findByUserId(Current.user().id)) try { m.put(p.k, json.readValue(p.v, Object.class)); } catch (Exception ignored) {}
        return m;
    }
    @PutMapping("/api/account/prefs/{key}") @Transactional
    public Object setPref(@PathVariable String key, @RequestBody Object value) throws Exception {
        if (!KEYS.containsKey(key)) throw ApiException.bad("Khoá tuỳ chọn không hợp lệ");
        int max = KEYS.get(key);
        if (max == -2) { if (!(value instanceof String str) || !List.of("VND", "USD").contains(str)) throw ApiException.bad("Tiền tệ không hỗ trợ"); }
        else if (max < 0) { if (!(value instanceof String str) || !com.ares.shop.service.I18nService.LANGS.contains(str)) throw ApiException.bad("Ngôn ngữ không hỗ trợ"); }
        else if (max > 0) {
            if (!(value instanceof List<?> l) || l.size() > max || !l.stream().allMatch(x -> x instanceof Number)) throw ApiException.bad("Danh sách tối đa " + max + " mã sản phẩm");
        } else if (!(value instanceof Map<?, ?> mm) || !mm.values().stream().allMatch(v -> v instanceof Boolean)) throw ApiException.bad("Cấu hình widget không hợp lệ");
        String v = json.writeValueAsString(value); if (v.length() > 4000) throw ApiException.bad("Dữ liệu quá lớn");
        Long uid = Current.user().id;
        UserPref p = prefs.findByUserIdAndK(uid, key).orElseGet(() -> { var n = new UserPref(); n.userId = uid; n.k = key; return n; });
        p.v = v; p.updatedAt = LocalDateTime.now(); prefs.save(p); return value;
    }

    @PostMapping("/api/account/viewed/{productId}") @Transactional
    public Map<String, Object> viewed(@PathVariable Long productId) {
        if (!products.existsById(productId)) throw ApiException.notFound("Không tìm thấy sản phẩm");
        Long uid = Current.user().id;
        ProductView v = views.findByUserIdAndProductId(uid, productId).orElseGet(() -> { var n = new ProductView(); n.userId = uid; n.productId = productId; n.count = 0; return n; });
        v.count = Optional.ofNullable(v.count).orElse(0) + 1; v.viewedAt = LocalDateTime.now(); views.save(v);
        var all = views.findByUserIdOrderByViewedAtDesc(uid); if (all.size() > 30) views.deleteAll(all.subList(30, all.size()));
        return Map.of("ok", true);
    }
    @GetMapping("/api/account/viewed")
    public List<Product> recentlyViewed() {
        var ids = views.findTop20ByUserIdOrderByViewedAtDesc(Current.user().id).stream().map(v -> v.productId).toList();
        Map<Long, Product> pm = new HashMap<>(); products.findAllById(ids).forEach(p -> pm.put(p.id, p));
        return ids.stream().map(pm::get).filter(p -> p != null && p.active).toList();
    }
    @DeleteMapping("/api/account/viewed") @Transactional
    public void clearViewed() { views.deleteAll(views.findByUserIdOrderByViewedAtDesc(Current.user().id)); }

    // ===== Tác vụ nền (chỉ ADMIN) =====
    private static void admin() { if (Current.user().role != Enums.Role.ADMIN) throw new ApiException(HttpStatus.FORBIDDEN, "Chỉ quản trị viên"); }
    @GetMapping("/api/admin/jobs")
    public Map<String, Object> jobList() { admin(); return Map.of("jobs", JobService.JOBS, "runs", jobs.recent()); }
    @PostMapping("/api/admin/jobs/{name}/run")
    public Map<String, Object> runJob(@PathVariable String name) {
        admin(); if (!JobService.JOBS.containsKey(name)) throw ApiException.notFound("Không có tác vụ");
        return Map.of("result", jobs.run(name, "manual:" + Current.user().email));
    }
}
