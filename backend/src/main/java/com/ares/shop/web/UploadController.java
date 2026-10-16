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

    @org.springframework.beans.factory.annotation.Value("${app.upload.max-dimension:1920}") int maxDim;
    public record Optimized(byte[] bytes, String ext) {}

    static boolean magicOk(byte[] b, String ext) {
        if (b.length < 12) return false;
        return switch (ext) {
            case "jpg", "jpeg" -> (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8;
            case "png" -> (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
            case "gif" -> b[0] == 'G' && b[1] == 'I' && b[2] == 'F';
            case "webp" -> b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
            default -> false;
        };
    }

    /** Giảm kích thước cạnh dài ≤ maxDim, JPEG chất lượng 0.82 (bỏ metadata EXIF). PNG không trong suốt → JPEG. */
    public static Optimized optimize(byte[] in, String ext, int maxDim) throws IOException {
        var src = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(in)); if (src == null) return null;
        int w = src.getWidth(), h = src.getHeight(); double sc = Math.min(1.0, (double) maxDim / Math.max(w, h));
        int nw = Math.max(1, (int) Math.round(w * sc)), nh = Math.max(1, (int) Math.round(h * sc));
        boolean alpha = src.getColorModel().hasAlpha() && hasTransparency(src);
        var dst = new java.awt.image.BufferedImage(nw, nh, alpha ? java.awt.image.BufferedImage.TYPE_INT_ARGB : java.awt.image.BufferedImage.TYPE_INT_RGB);
        var g = dst.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        if (!alpha) { g.setColor(java.awt.Color.WHITE); g.fillRect(0, 0, nw, nh); }
        g.drawImage(src, 0, 0, nw, nh, null); g.dispose();
        var out = new java.io.ByteArrayOutputStream();
        if (alpha) { javax.imageio.ImageIO.write(dst, "png", out); return new Optimized(out.toByteArray(), "png"); }
        var wr = javax.imageio.ImageIO.getImageWritersByFormatName("jpg").next(); var p = wr.getDefaultWriteParam();
        p.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT); p.setCompressionQuality(0.82f);
        try (var ios = javax.imageio.ImageIO.createImageOutputStream(out)) { wr.setOutput(ios); wr.write(null, new javax.imageio.IIOImage(dst, null, null), p); } finally { wr.dispose(); }
        return new Optimized(out.toByteArray(), "jpg");
    }
    static boolean hasTransparency(java.awt.image.BufferedImage im) {
        int stepX = Math.max(1, im.getWidth() / 64), stepY = Math.max(1, im.getHeight() / 64);
        for (int y = 0; y < im.getHeight(); y += stepY) for (int x = 0; x < im.getWidth(); x += stepX) if ((im.getRGB(x, y) >>> 24) < 250) return true;
        return false;
    }

    @PostMapping("/api/uploads")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        Current.user();
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) throw ApiException.bad("Ảnh tối đa 5MB");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("x.jpg");
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!OK.contains(ext)) throw ApiException.bad("Chỉ nhận ảnh JPG, PNG, WEBP, GIF");
        Files.createDirectories(DIR);
        byte[] in = file.getBytes();
        if (!magicOk(in, ext)) throw ApiException.bad("Tệp không phải ảnh hợp lệ");
        // v16: thu nhỏ + nén lại JPG/PNG lớn phía server (giữ PNG có nền trong suốt); WEBP/GIF giữ nguyên.
        byte[] outBytes = in; String outExt = ext.equals("jpeg") ? "jpg" : ext;
        if (Set.of("jpg", "jpeg", "png").contains(ext)) {
            try {
                var r = optimize(in, ext, maxDim);
                if (r != null && r.bytes().length < in.length) { outBytes = r.bytes(); outExt = r.ext(); }
            } catch (Exception ignored) { /* ảnh lạ → lưu bản gốc */ }
        }
        String fn = UUID.randomUUID().toString().substring(0, 12) + "." + outExt;
        Files.write(DIR.resolve(fn), outBytes);
        return Map.of("url", "/uploads/" + fn, "bytes", String.valueOf(outBytes.length), "originalBytes", String.valueOf(in.length));
    }
}
