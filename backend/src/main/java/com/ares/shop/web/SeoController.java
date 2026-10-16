package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * SEO: sitemap.xml, robots.txt và bản HTML có sẵn thẻ meta/OG/JSON-LD cho bot (prerender nhẹ, không cần headless browser):
 * reverse proxy chuyển request của crawler (Googlebot, facebookexternalhit, Zalo…) tới /api/public/seo/render?path=… (xem docs/SEO.md).
 */
@RestController
public class SeoController {
    private final ProductRepo products; private final PostRepo posts; private final LookbookRepo looks; private final String base; private final Path dist;
    private final ObjectMapper om = new ObjectMapper().findAndRegisterModules();
    public SeoController(ProductRepo products, PostRepo posts, LookbookRepo looks, @Value("${app.public-base-url}") String base, @Value("${app.static-dist:../frontend/dist}") String dist) {
        this.products = products; this.posts = posts; this.looks = looks; this.base = base.replaceAll("/+$", ""); this.dist = Paths.get(dist).toAbsolutePath().normalize();
    }
    static final String SITE = "ARES Toys", DEF_DESC = "Xe mô hình, xe RC và phụ kiện chính hãng — Hot Wheels, Mini GT, Tamiya, Traxxas, LEGO. Giao nhanh toàn quốc, đổi trả 7 ngày.";

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return "User-agent: *\nAllow: /\nDisallow: /admin\nDisallow: /account\nDisallow: /checkout\nDisallow: /cart\nDisallow: /pay/\nDisallow: /orders\nDisallow: /api/\nDisallow: /oauth-callback\nAllow: /api/public/seo/\n\nSitemap: " + base + "/sitemap.xml\n";
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        StringBuilder x = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\" xmlns:image=\"http://www.google.com/schemas/sitemap-image/1.1\">\n");
        for (String[] s : new String[][]{{"/", "daily", "1.0"}, {"/products", "daily", "0.9"}, {"/blog", "daily", "0.8"}, {"/collections", "weekly", "0.7"}, {"/promotions", "daily", "0.7"},
            {"/auctions", "daily", "0.6"}, {"/bundles", "weekly", "0.6"}, {"/faq", "monthly", "0.4"}, {"/about", "monthly", "0.3"}, {"/support", "monthly", "0.3"}})
            x.append(url(s[0], null, s[1], s[2], null));
        DateTimeFormatter d = DateTimeFormatter.ISO_LOCAL_DATE;
        for (Product p : products.findAll()) if (p.active) x.append(url("/products/" + p.id, p.createdAt == null ? null : p.createdAt.format(d), "weekly", "0.8", p.images.isEmpty() ? null : abs(p.images.get(0))));
        for (Post p : posts.findByStatusOrderByPublishedAtDesc("PUBLISHED")) if (CmsController.live(p)) x.append(url("/blog/" + p.slug, p.updatedAt.format(d), "monthly", "0.6", p.coverImage == null ? null : abs(p.coverImage)));
        for (Lookbook l : looks.findAll()) if (l.slug != null) x.append(url("/collections/" + l.slug, null, "weekly", "0.5", null));
        x.append("</urlset>\n");
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600").body(x.toString());
    }
    String url(String path, String lastmod, String freq, String prio, String img) {
        return "  <url><loc>" + esc(base + path) + "</loc>" + (lastmod == null ? "" : "<lastmod>" + lastmod + "</lastmod>") + "<changefreq>" + freq + "</changefreq><priority>" + prio + "</priority>"
            + (img == null ? "" : "<image:image><image:loc>" + esc(img) + "</image:loc></image:image>") + "</url>\n";
    }
    String abs(String u) { return u.startsWith("http") ? u : base + u; }
    static String esc(String s) { return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }

    public record Meta(String title, String description, String image, String type, String canonical, List<Object> jsonLd) {}
    /** Meta cho một đường dẫn của SPA (dùng chung cho bot render và kiểm thử). */
    @GetMapping("/api/public/seo/meta")
    public Meta meta(@RequestParam String path) {
        String p = path.split("[?#]")[0]; if (!p.startsWith("/")) p = "/" + p;
        List<Object> ld = new ArrayList<>();
        Map<String, Object> orgLd = Map.of("@context", "https://schema.org", "@type", "Organization", "name", SITE, "url", base, "logo", base + "/icons/icon-512.png");
        var m = java.util.regex.Pattern.compile("^/products/(\\d+)$").matcher(p);
        if (m.find()) {
            Product pr = products.findById(Long.parseLong(m.group(1))).filter(x -> x.active).orElse(null);
            if (pr != null) {
                String img = pr.images.isEmpty() ? null : abs(pr.images.get(0)), desc = trim(pr.description, 160);
                Map<String, Object> prod = new LinkedHashMap<>(); prod.put("@context", "https://schema.org"); prod.put("@type", "Product"); prod.put("name", pr.name); prod.put("sku", pr.sku);
                prod.put("image", pr.images.stream().map(this::abs).toList()); prod.put("description", desc); if (pr.brand != null) prod.put("brand", Map.of("@type", "Brand", "name", pr.brand.name));
                prod.put("offers", Map.of("@type", "Offer", "url", base + p, "priceCurrency", "VND", "price", pr.price, "availability", "https://schema.org/" + (pr.stock > 0 ? "InStock" : Boolean.TRUE.equals(pr.preorder) ? "PreOrder" : "OutOfStock"), "itemCondition", "https://schema.org/NewCondition"));
                if (pr.reviewCount > 0) prod.put("aggregateRating", Map.of("@type", "AggregateRating", "ratingValue", pr.rating, "reviewCount", pr.reviewCount));
                ld.add(prod); ld.add(crumbs(List.of(new String[]{"Trang chủ", "/"}, new String[]{"Sản phẩm", "/products"}, new String[]{pr.name, p})));
                return new Meta(pr.name + " | " + SITE, desc, img, "product", base + p, ld);
            }
        }
        var b = java.util.regex.Pattern.compile("^/blog/([a-z0-9-]+)$").matcher(p);
        if (b.find()) {
            Post po = posts.findBySlug(b.group(1)).filter(CmsController::live).orElse(null);
            if (po != null) {
                String desc = po.seoDescription != null ? po.seoDescription : po.excerpt != null ? po.excerpt : trim(org.jsoup.Jsoup.parse(String.valueOf(po.contentHtml)).text(), 160);
                Map<String, Object> art = new LinkedHashMap<>(); art.put("@context", "https://schema.org"); art.put("@type", "VIDEO".equals(po.kind) ? "VideoObject" : "NEWS".equals(po.kind) ? "NewsArticle" : "BlogPosting");
                if ("VIDEO".equals(po.kind)) { art.put("name", po.title); art.put("description", desc); art.put("thumbnailUrl", po.coverImage == null ? List.of() : List.of(abs(po.coverImage))); art.put("uploadDate", po.publishedAt.toString()); art.put("embedUrl", po.videoUrl); }
                else { art.put("headline", po.title); art.put("description", desc); if (po.coverImage != null) art.put("image", List.of(abs(po.coverImage))); art.put("datePublished", po.publishedAt.toString()); art.put("dateModified", po.updatedAt.toString());
                    art.put("author", Map.of("@type", "Person", "name", po.author == null ? SITE : po.author)); art.put("publisher", orgLd); art.put("mainEntityOfPage", base + p); }
                ld.add(art); ld.add(crumbs(List.of(new String[]{"Trang chủ", "/"}, new String[]{"Tin tức", "/blog"}, new String[]{po.title, p})));
                return new Meta((po.seoTitle != null ? po.seoTitle : po.title) + " | " + SITE, desc, po.coverImage == null ? null : abs(po.coverImage), "article", base + p, ld);
            }
        }
        Map<String, String[]> st = Map.of("/products", new String[]{"Tất cả sản phẩm", "Xe mô hình 1:64–1:18, xe RC, đồ chơi lắp ráp và phụ kiện chính hãng."},
            "/blog", new String[]{"Tin tức & Review", "Tin tức, bài hướng dẫn và video review xe mô hình, xe RC từ ARES Toys."},
            "/collections", new String[]{"Bộ sưu tập", "Những bộ sưu tập xe mô hình được tuyển chọn."}, "/promotions", new String[]{"Khuyến mãi", "Mã giảm giá, flash sale và ưu đãi đang diễn ra."},
            "/auctions", new String[]{"Đấu giá hàng hiếm", "Đấu giá xe mô hình hiếm, bản giới hạn."}, "/faq", new String[]{"Câu hỏi thường gặp", "Giải đáp về giao hàng, đổi trả, bảo hành, thanh toán."},
            "/about", new String[]{"Về chúng tôi", DEF_DESC});
        if (st.containsKey(p)) return new Meta(st.get(p)[0] + " | " + SITE, st.get(p)[1], null, "website", base + p, List.of(orgLd));
        ld.add(orgLd); ld.add(Map.of("@context", "https://schema.org", "@type", "WebSite", "name", SITE, "url", base, "potentialAction", Map.of("@type", "SearchAction", "target", base + "/products?q={search_term_string}", "query-input", "required name=search_term_string")));
        return new Meta(SITE + " — Xe mô hình & xe RC chính hãng", DEF_DESC, null, "website", base + ("/".equals(p) ? "/" : p), ld);
    }
    Map<String, Object> crumbs(List<String[]> items) {
        List<Map<String, Object>> l = new ArrayList<>(); for (int i = 0; i < items.size(); i++) l.add(Map.of("@type", "ListItem", "position", i + 1, "name", items.get(i)[0], "item", base + items.get(i)[1]));
        return Map.of("@context", "https://schema.org", "@type", "BreadcrumbList", "itemListElement", l);
    }
    static String trim(String s, int n) { if (s == null) return DEF_DESC; s = s.replaceAll("\\s+", " ").trim(); return s.length() <= n ? s : s.substring(0, n - 1).replaceAll("\\s+\\S*$", "") + "…"; }

    /** index.html của SPA kèm meta/OG/Twitter/JSON-LD đã điền sẵn — cho crawler. */
    @GetMapping(value = "/api/public/seo/render", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> render(@RequestParam String path) throws Exception {
        Meta m = meta(path); Path idx = dist.resolve("index.html");
        String html = Files.isRegularFile(idx) ? Files.readString(idx) : "<!doctype html><html lang=\"vi\"><head><meta charset=\"utf-8\"><title>ARES Toys</title></head><body><div id=\"app\"></div></body></html>";
        String img = m.image() != null ? m.image() : base + "/icons/icon-512.png";
        StringBuilder h = new StringBuilder();
        h.append("<title>").append(esc(m.title())).append("</title>\n<meta name=\"description\" content=\"").append(esc(m.description())).append("\">\n<link rel=\"canonical\" href=\"").append(esc(m.canonical())).append("\">\n");
        for (String[] kv : new String[][]{{"og:title", m.title()}, {"og:description", m.description()}, {"og:image", img}, {"og:url", m.canonical()}, {"og:type", m.type()}, {"og:site_name", SITE}, {"og:locale", "vi_VN"}})
            h.append("<meta property=\"").append(kv[0]).append("\" content=\"").append(esc(kv[1])).append("\">\n");
        h.append("<meta name=\"twitter:card\" content=\"summary_large_image\">\n");
        for (Object o : m.jsonLd()) h.append("<script type=\"application/ld+json\">").append(om.writeValueAsString(o).replace("</", "<\\/")).append("</script>\n");
        html = html.replaceFirst("(?s)<title>.*?</title>", "").replaceFirst("(?i)</head>", java.util.regex.Matcher.quoteReplacement(h + "</head>"));
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "public, max-age=600").body(html);
    }
}
