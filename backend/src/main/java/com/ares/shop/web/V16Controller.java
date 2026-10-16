package com.ares.shop.web;

import com.ares.shop.service.ImageSearchService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
public class V16Controller {
    private final ImageSearchService images;
    public V16Controller(ImageSearchService images) { this.images = images; }

    static final Set<String> IMG = Set.of("image/jpeg", "image/png", "image/webp", "image/gif", "image/bmp");
    /** Tải ảnh lên → sản phẩm tương tự. Ảnh không được lưu lại. */
    @PostMapping("/api/public/image-search")
    public Map<String, Object> imageSearch(@RequestParam("file") MultipartFile f, @RequestParam(defaultValue = "8") int limit) throws Exception {
        if (f == null || f.isEmpty()) throw ApiException.bad("Chưa chọn ảnh");
        if (f.getSize() > 6 * 1024 * 1024) throw ApiException.bad("Ảnh tối đa 6MB");
        if (f.getContentType() == null || !IMG.contains(f.getContentType())) throw ApiException.bad("Chỉ nhận ảnh JPG, PNG, WEBP, GIF");
        try { return images.search(f.getBytes(), f.getContentType(), Math.max(1, Math.min(limit, 24))); }
        catch (IllegalArgumentException e) { throw ApiException.bad("Không đọc được ảnh (định dạng không hỗ trợ)"); }
    }
    @PostMapping("/api/admin/image-search/reindex") public Map<String, Object> reindex() { return images.reindex(); }
    @GetMapping("/api/admin/image-search/status") public Map<String, Object> status() { return Map.of("indexed", images.indexed, "failed", images.failed, "indexedAt", String.valueOf(images.indexedAt)); }
}
