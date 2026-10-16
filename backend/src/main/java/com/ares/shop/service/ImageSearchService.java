package com.ares.shop.service;

import com.ares.shop.model.Product;
import com.ares.shop.repo.ProductRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tìm sản phẩm bằng ảnh, chạy hoàn toàn phía server (không cần dịch vụ ngoài):
 * đặc trưng mỗi ảnh = dHash 64-bit (hình dạng/bố cục) + histogram màu HSV 8×3×3 (72 bin) có trọng số vùng trung tâm.
 * Điểm = 0.55·giao histogram + 0.45·(1 − hamming/64). Nếu có AI key và model hỗ trợ ảnh: mô tả ảnh → từ khoá → cộng điểm sản phẩm khớp.
 */
@Service
public class ImageSearchService {
    record Feat(long dhash, float[] hist) {}
    private final ProductRepo products; private final SearchService search; private final AiService ai;
    private final Path staticDir; private final String publicBase;
    private final Map<String, Feat> cache = new ConcurrentHashMap<>();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    public volatile int indexed, failed; public volatile java.time.LocalDateTime indexedAt;

    public ImageSearchService(ProductRepo products, SearchService search, AiService ai, @Value("${app.static-dir:../frontend/public}") String staticDir, @Value("${app.public-base-url}") String publicBase) {
        this.products = products; this.search = search; this.ai = ai; this.staticDir = Paths.get(staticDir).toAbsolutePath().normalize(); this.publicBase = publicBase.replaceAll("/+$", "");
    }
    @EventListener(ApplicationReadyEvent.class)
    public void warm() { Thread t = new Thread(this::reindex, "img-index"); t.setDaemon(true); t.start(); }

    public synchronized Map<String, Object> reindex() {
        int ok = 0, bad = 0;
        for (Product p : products.findAll()) for (String url : p.images) { if (cache.containsKey(url)) { ok++; continue; } Feat f = load(url); if (f != null) { cache.put(url, f); ok++; } else bad++; }
        indexed = ok; failed = bad; indexedAt = java.time.LocalDateTime.now();
        return Map.of("indexed", ok, "failed", bad, "staticDir", staticDir.toString());
    }
    Feat load(String url) {
        try {
            byte[] b = null;
            if (url.startsWith("/uploads/")) { Path f = Paths.get("uploads").toAbsolutePath().resolve(url.substring(9)).normalize(); if (Files.isRegularFile(f)) b = Files.readAllBytes(f); }
            else if (url.startsWith("/")) { Path f = staticDir.resolve(url.substring(1)).normalize(); if (f.startsWith(staticDir) && Files.isRegularFile(f)) b = Files.readAllBytes(f); }
            if (b == null && url.startsWith("/")) { // fallback: lấy qua HTTP từ site
                HttpResponse<byte[]> r = http.send(HttpRequest.newBuilder(URI.create(publicBase + url)).timeout(Duration.ofSeconds(8)).build(), HttpResponse.BodyHandlers.ofByteArray());
                if (r.statusCode() == 200) b = r.body();
            }
            return b == null ? null : features(b);
        } catch (Exception e) { return null; }
    }
    public static Feat features(byte[] bytes) throws Exception {
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(bytes));
        if (src == null) throw new IllegalArgumentException("Không đọc được ảnh");
        BufferedImage g = scale(src, 9, 8); long h = 0; int bit = 0;
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) { if (lum(g.getRGB(x, y)) > lum(g.getRGB(x + 1, y))) h |= 1L << bit; bit++; }
        BufferedImage s = scale(src, 48, 48); float[] hist = new float[72]; float tot = 0; float[] hsv = new float[3];
        for (int y = 0; y < 48; y++) for (int x = 0; x < 48; x++) {
            int c = s.getRGB(x, y); java.awt.Color.RGBtoHSB((c >> 16) & 255, (c >> 8) & 255, c & 255, hsv);
            if (hsv[1] < 0.12f && hsv[2] > 0.9f) continue; // bỏ nền trắng studio
            double dx = (x - 23.5) / 24, dy = (y - 23.5) / 24; float w = (float) Math.max(0.25, 1 - Math.sqrt(dx * dx + dy * dy));
            int hb = Math.min(7, (int) (hsv[0] * 8)), sb = Math.min(2, (int) (hsv[1] * 3)), vb = Math.min(2, (int) (hsv[2] * 3));
            hist[hb * 9 + sb * 3 + vb] += w; tot += w;
        }
        if (tot > 0) for (int i = 0; i < 72; i++) hist[i] /= tot;
        return new Feat(h, hist);
    }
    static BufferedImage scale(BufferedImage src, int w, int h) {
        BufferedImage o = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB); Graphics2D g = o.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); g.setColor(java.awt.Color.WHITE); g.fillRect(0, 0, w, h); g.drawImage(src, 0, 0, w, h, null); g.dispose(); return o;
    }
    static int lum(int c) { return (((c >> 16) & 255) * 299 + ((c >> 8) & 255) * 587 + (c & 255) * 114) / 1000; }
    static double sim(Feat a, Feat b) {
        double inter = 0; for (int i = 0; i < 72; i++) inter += Math.min(a.hist[i], b.hist[i]);
        return 0.55 * inter + 0.45 * (1 - Long.bitCount(a.dhash ^ b.dhash) / 64.0);
    }

    public Map<String, Object> search(byte[] bytes, String mime, int limit) throws Exception {
        if (cache.isEmpty()) reindex();
        Feat q = features(bytes);
        Map<Long, Double> best = new HashMap<>(); Map<Long, Product> byId = new HashMap<>();
        for (Product p : products.findAll()) { if (!p.active) continue; byId.put(p.id, p);
            for (String url : p.images) { Feat f = cache.get(url); if (f != null) best.merge(p.id, sim(q, f), Math::max); } }
        String keywords = null;
        if (ai.enabled()) { try { keywords = ai.describeImage(bytes, mime); if (keywords != null && !keywords.isBlank()) { int rank = 0;
            for (Product p : search.search(keywords, true, false)) { if (rank >= 8) break; best.merge(p.id, 0.25 - rank * 0.02, Double::sum); rank++; } } } catch (Exception ignored) { } }
        List<Map<String, Object>> out = best.entrySet().stream().sorted(Map.Entry.<Long, Double>comparingByValue().reversed()).limit(limit).map(e -> {
            Product p = byId.get(e.getKey()); Map<String, Object> m = new LinkedHashMap<>(); m.put("id", p.id); m.put("name", p.name); m.put("price", p.price); m.put("oldPrice", p.oldPrice);
            m.put("image", p.images.isEmpty() ? "" : p.images.get(0)); m.put("brand", p.brand == null ? null : p.brand.name); m.put("scale", p.scale); m.put("stock", p.stock); m.put("rating", p.rating);
            m.put("images", p.images); m.put("type", p.type); m.put("hasVariants", p.hasVariants); m.put("reviewCount", p.reviewCount); m.put("isNew", p.isNew); m.put("ageMin", p.ageMin);
            m.put("score", Math.round(e.getValue() * 1000) / 1000.0); return m; }).toList();
        Map<String, Object> r = new LinkedHashMap<>(); r.put("results", out); r.put("indexed", cache.size()); r.put("ai", keywords != null); if (keywords != null) r.put("keywords", keywords);
        return r;
    }
}
