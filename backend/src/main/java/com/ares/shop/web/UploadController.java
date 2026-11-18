package com.ares.shop.web;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

/**
 * Quan ly tai len va thu vien hinh anh san pham, banner, danh gia tai /uploads/**.
 */
@RestController
public class UploadController {
    public static final Path DIR = Paths.get("uploads").toAbsolutePath();
    private static final Set<String> OK = Set.of("jpg", "jpeg", "png", "webp", "gif", "svg");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    @GetMapping("/api/uploads")
    public List<Map<String, Object>> listMedia(@RequestParam(required = false) String q) {
        Current.user();
        if (!Files.exists(DIR)) return List.of();
        List<Map<String, Object>> res = new ArrayList<>();
        try (Stream<Path> stream = Files.list(DIR)) {
            stream.filter(Files::isRegularFile).forEach(p -> {
                String name = p.getFileName().toString();
                String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
                if (!OK.contains(ext)) return;
                if (q != null && !q.isBlank() && !name.toLowerCase().contains(q.trim().toLowerCase())) return;

                try {
                    BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", name);
                    m.put("url", "/uploads/" + name);
                    m.put("size", attrs.size());
                    m.put("ext", ext.toUpperCase());
                    m.put("lastModified", attrs.lastModifiedTime().toMillis());
                    m.put("updatedAt", ISO.format(attrs.lastModifiedTime().toInstant()));
                    res.add(m);
                } catch (Exception ignored) {}
            });
        } catch (IOException e) {
            throw ApiException.bad("Khong the doc thu muc anh: " + e.getMessage());
        }
        res.sort((a, b) -> Long.compare((Long) b.get("lastModified"), (Long) a.get("lastModified")));
        return res;
    }

    @PostMapping("/api/uploads")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        Current.user();
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024) throw ApiException.bad("Anh toi da 10MB");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("image.jpg");
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!OK.contains(ext)) throw ApiException.bad("Chi nhan dinh dang anh JPG, PNG, WEBP, GIF, SVG");
        Files.createDirectories(DIR);
        String fn = UUID.randomUUID().toString().substring(0, 12) + "." + ext;
        file.transferTo(DIR.resolve(fn));
        return Map.of("url", "/uploads/" + fn, "name", fn, "originalName", name);
    }

    @PostMapping("/api/uploads/batch")
    public List<Map<String, String>> uploadBatch(@RequestParam("files") MultipartFile[] files) throws IOException {
        Current.user();
        if (files == null || files.length == 0) throw ApiException.bad("Vui long chon it nhat 1 file anh");
        Files.createDirectories(DIR);
        List<Map<String, String>> list = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String name = Optional.ofNullable(file.getOriginalFilename()).orElse("image.jpg");
            String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
            if (!OK.contains(ext)) continue;
            String fn = UUID.randomUUID().toString().substring(0, 12) + "." + ext;
            file.transferTo(DIR.resolve(fn));
            list.add(Map.of("url", "/uploads/" + fn, "name", fn, "originalName", name));
        }
        return list;
    }

    @DeleteMapping("/api/uploads/{filename}")
    public Map<String, Object> deleteImage(@PathVariable String filename) {
        Current.user();
        if (filename == null || filename.isBlank() || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw ApiException.bad("Ten file khong hop le");
        }
        Path target = DIR.resolve(filename).normalize();
        if (!target.startsWith(DIR)) {
            throw ApiException.bad("Duong dan file khong hop le");
        }
        try {
            boolean deleted = Files.deleteIfExists(target);
            return Map.of("success", deleted, "filename", filename);
        } catch (IOException e) {
            throw ApiException.bad("Loi khi xoa anh: " + e.getMessage());
        }
    }
}
