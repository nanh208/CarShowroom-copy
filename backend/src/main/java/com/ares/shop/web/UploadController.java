package com.ares.shop.web;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Tải ảnh (đánh giá, đổi trả, sản phẩm, banner) lên thư mục ./uploads, phục vụ tại /uploads/**. */
@RestController
public class UploadController {
    public static final Path DIR = Paths.get("uploads").toAbsolutePath();
    private static final Set<String> OK = Set.of("jpg", "jpeg", "png", "webp", "gif");

    @PostMapping("/api/uploads")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        Current.user();
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) throw ApiException.bad("Ảnh tối đa 5MB");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("x.jpg");
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!OK.contains(ext)) throw ApiException.bad("Chỉ nhận ảnh JPG, PNG, WEBP, GIF");
        Files.createDirectories(DIR);
        String fn = UUID.randomUUID().toString().substring(0, 12) + "." + ext;
        file.transferTo(DIR.resolve(fn));
        return Map.of("url", "/uploads/" + fn);
    }
}
