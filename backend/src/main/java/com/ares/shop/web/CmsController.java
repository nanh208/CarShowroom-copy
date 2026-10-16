package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.*;

@RestController
public class CmsController {
    private final PostRepo posts; private final ProductRepo products;
    public CmsController(PostRepo posts, ProductRepo products) { this.posts = posts; this.products = products; }

    /** Thẻ HTML cho phép trong bài viết (rich text) — loại script/style/on*, iframe chỉ YouTube/Vimeo. */
    static final Safelist SAFE = Safelist.relaxed().addTags("figure", "figcaption", "hr", "iframe").addAttributes("iframe", "src", "width", "height", "allowfullscreen", "title", "loading")
        .addAttributes("img", "loading", "width", "height").addAttributes("a", "target", "rel").addProtocols("img", "src", "http", "https").removeProtocols("a", "href", "ftp", "mailto")
        .addEnforcedAttribute("a", "rel", "noopener nofollow");
    public static String clean(String html) {
        if (html == null) return null;
        String c = Jsoup.clean(html, "https://ares.local", SAFE, new org.jsoup.nodes.Document.OutputSettings().prettyPrint(false));
        var doc = Jsoup.parseBodyFragment(c);
        doc.select("iframe").forEach(f -> { String s = f.attr("src"); if (!s.matches("^https://(www\\.youtube(-nocookie)?\\.com/embed/|player\\.vimeo\\.com/video/)[\\w\\-?=&]+$")) f.remove(); else f.attr("loading", "lazy"); });
        doc.select("img").forEach(i -> { String s = i.attr("src"); if (!(s.startsWith("/") || s.startsWith("https://"))) i.remove(); else i.attr("loading", "lazy"); });
        doc.select("a[href]").forEach(a -> { String h = a.attr("href"); if (!(h.startsWith("/") || h.startsWith("https://") || h.startsWith("http://") || h.startsWith("#"))) a.removeAttr("href"); });
        return doc.body().html();
    }
    public static String slugify(String s) {
        String n = Normalizer.normalize(s == null ? "" : s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace('đ', 'd');
        n = n.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", ""); return n.length() > 80 ? n.substring(0, 80).replaceAll("-$", "") : n;
    }
    /** URL video → URL nhúng an toàn. */
    public static String embed(String v) {
        if (v == null || v.isBlank()) return null;
        var m = java.util.regex.Pattern.compile("(?:youtube\\.com/(?:watch\\?v=|embed/|shorts/)|youtu\\.be/)([\\w-]{11})").matcher(v);
        if (m.find()) return "https://www.youtube-nocookie.com/embed/" + m.group(1);
        var vm = java.util.regex.Pattern.compile("vimeo\\.com/(?:video/)?(\\d+)").matcher(v);
        if (vm.find()) return "https://player.vimeo.com/video/" + vm.group(1);
        if (v.matches("^(/|https://)\\S+\\.(mp4|webm)$")) return v;
        throw ApiException.bad("Video phải là link YouTube, Vimeo hoặc tệp .mp4/.webm");
    }
    static int readMin(String html) { int w = html == null ? 0 : Jsoup.parse(html).text().split("\\s+").length; return Math.max(1, Math.round(w / 220f)); }
    static boolean live(Post p) { return "PUBLISHED".equals(p.status) && p.publishedAt != null && !p.publishedAt.isAfter(LocalDateTime.now()); }
    Map<String, Object> card(Post p) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("id", p.id); m.put("slug", p.slug); m.put("title", p.title); m.put("titleEn", p.titleEn); m.put("excerpt", p.excerpt); m.put("excerptEn", p.excerptEn);
        m.put("coverImage", p.coverImage); m.put("kind", p.kind); m.put("tags", p.tags); m.put("publishedAt", p.publishedAt); m.put("author", p.author); m.put("views", p.views); m.put("readMinutes", readMin(p.contentHtml)); m.put("hasVideo", p.videoUrl != null);
        return m;
    }

    // ---------- public ----------
    @GetMapping("/api/public/posts")
    public Map<String, Object> list(@RequestParam(required = false) String kind, @RequestParam(required = false) String tag, @RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "9") int size) {
        String nq = q == null ? "" : BotController.norm(q);
        List<Post> all = posts.findByStatusOrderByPublishedAtDesc("PUBLISHED").stream().filter(CmsController::live)
            .filter(p -> kind == null || kind.isBlank() || kind.equalsIgnoreCase(p.kind)).filter(p -> tag == null || tag.isBlank() || p.tags.contains(tag))
            .filter(p -> nq.isEmpty() || BotController.norm(p.title + " " + p.excerpt + " " + String.join(" ", p.tags)).contains(nq)).toList();
        size = Math.max(1, Math.min(size, 30));
        return Map.of("content", all.stream().skip((long) page * size).limit(size).map(this::card).toList(), "totalElements", all.size(), "page", page, "size", size);
    }
    @GetMapping("/api/public/posts/tags")
    public List<Map<String, Object>> tags() {
        Map<String, Long> c = posts.findByStatusOrderByPublishedAtDesc("PUBLISHED").stream().filter(CmsController::live).flatMap(p -> p.tags.stream()).collect(Collectors.groupingBy(t -> t, Collectors.counting()));
        return c.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(30).map(e -> Map.<String, Object>of("tag", e.getKey(), "count", e.getValue())).toList();
    }
    @GetMapping("/api/public/posts/{slug}") @Transactional
    public Map<String, Object> get(@PathVariable String slug, @RequestParam(defaultValue = "true") boolean count) {
        Post p = posts.findBySlug(slug).filter(CmsController::live).orElseThrow(() -> ApiException.notFound("Không tìm thấy bài viết"));
        if (count) { p.views++; posts.save(p); }
        Map<String, Object> m = card(p); m.put("contentHtml", p.contentHtml); m.put("contentHtmlEn", p.contentHtmlEn); m.put("videoUrl", p.videoUrl); m.put("seoTitle", p.seoTitle); m.put("seoDescription", p.seoDescription); m.put("updatedAt", p.updatedAt);
        m.put("products", p.productIds.isEmpty() ? List.of() : products.findAllById(p.productIds).stream().filter(x -> x.active).toList());
        m.put("related", posts.findByStatusOrderByPublishedAtDesc("PUBLISHED").stream().filter(CmsController::live).filter(o -> !o.id.equals(p.id))
            .sorted(Comparator.comparingLong((Post o) -> -o.tags.stream().filter(p.tags::contains).count())).limit(3).map(this::card).toList());
        return m;
    }

    // ---------- admin ----------
    @GetMapping("/api/admin/posts") public List<Map<String, Object>> adminList() {
        return posts.findAllByOrderByIdDesc().stream().map(p -> { Map<String, Object> m = card(p); m.put("status", p.status); m.put("updatedAt", p.updatedAt); return m; }).toList();
    }
    @GetMapping("/api/admin/posts/{id}") public Post adminGet(@PathVariable Long id) { return posts.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy bài viết")); }
    @PostMapping("/api/admin/posts") @Transactional public Post create(@RequestBody Post in) { return save(new Post(), in); }
    @PutMapping("/api/admin/posts/{id}") @Transactional public Post update(@PathVariable Long id, @RequestBody Post in) { return save(adminGet(id), in); }
    @DeleteMapping("/api/admin/posts/{id}") public void delete(@PathVariable Long id) { posts.deleteById(id); }

    Post save(Post p, Post in) {
        if (in.title == null || in.title.isBlank()) throw ApiException.bad("Tiêu đề bắt buộc");
        if (!Set.of("NEWS", "BLOG", "VIDEO").contains(String.valueOf(in.kind))) throw ApiException.bad("Loại bài không hợp lệ");
        if (!Set.of("DRAFT", "PUBLISHED").contains(String.valueOf(in.status))) throw ApiException.bad("Trạng thái không hợp lệ");
        p.title = in.title.trim(); p.titleEn = blank(in.titleEn); p.excerpt = blank(in.excerpt); p.excerptEn = blank(in.excerptEn); p.kind = in.kind;
        p.contentHtml = clean(in.contentHtml); p.contentHtmlEn = clean(blank(in.contentHtmlEn));
        p.coverImage = blank(in.coverImage); if (p.coverImage != null && !(p.coverImage.startsWith("/") || p.coverImage.startsWith("https://"))) throw ApiException.bad("Ảnh bìa phải là URL / hoặc https://");
        p.videoUrl = embed(in.videoUrl); if ("VIDEO".equals(p.kind) && p.videoUrl == null) throw ApiException.bad("Bài video cần link video");
        p.tags = in.tags == null ? new LinkedHashSet<>() : in.tags.stream().map(t -> t.trim().toLowerCase(Locale.ROOT)).filter(t -> !t.isEmpty() && t.length() <= 40).limit(12).collect(Collectors.toCollection(LinkedHashSet::new));
        p.productIds = in.productIds == null ? new ArrayList<>() : in.productIds.stream().filter(Objects::nonNull).distinct().limit(8).collect(Collectors.toCollection(ArrayList::new));
        p.seoTitle = blank(in.seoTitle); p.seoDescription = blank(in.seoDescription);
        String slug = slugify(in.slug == null || in.slug.isBlank() ? in.title : in.slug); if (slug.isEmpty()) slug = "bai-viet";
        String base = slug; int i = 2; while (posts.findBySlug(slug).filter(o -> !o.id.equals(p.id)).isPresent()) slug = base + "-" + i++;
        p.slug = slug;
        if ("PUBLISHED".equals(in.status) && p.publishedAt == null) p.publishedAt = in.publishedAt != null ? in.publishedAt : LocalDateTime.now();
        else if (in.publishedAt != null) p.publishedAt = in.publishedAt;
        p.status = in.status; if (p.author == null) p.author = Current.user().fullName; p.updatedAt = LocalDateTime.now();
        return posts.save(p);
    }
    static String blank(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
