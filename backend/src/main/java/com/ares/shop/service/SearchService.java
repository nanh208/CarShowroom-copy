package com.ares.shop.service;

import com.ares.shop.model.Product;
import com.ares.shop.repo.ProductRepo;
import com.ares.shop.web.BotController;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.*;

/**
 * Tìm kiếm thông minh dùng chung cho trang sản phẩm, autocomplete và chatbot:
 * bỏ dấu, từ đồng nghĩa, chịu lỗi gõ (Levenshtein), trích thực thể tỉ lệ / thương hiệu / giá / tuổi.
 */
@Service
public class SearchService {
    private final ProductRepo products;
    public SearchService(ProductRepo products) { this.products = products; }

    /** Từ đồng nghĩa → dạng chuẩn (đều đã bỏ dấu). */
    static final Map<String, String> SYN = Map.ofEntries(
        Map.entry("oto", "xe"), Map.entry("o to", "xe"), Map.entry("car", "xe"), Map.entry("cars", "xe"),
        Map.entry("lambo", "lamborghini"), Map.entry("merc", "mercedes"), Map.entry("benz", "mercedes"), Map.entry("mec", "mercedes"),
        Map.entry("vw", "volkswagen"), Map.entry("chevy", "chevrolet"), Map.entry("beemer", "bmw"), Map.entry("hw", "hot wheels"),
        Map.entry("hotwheels", "hot wheels"), Map.entry("dieu khien", "rc"), Map.entry("remote", "rc"), Map.entry("xe dieu khien", "rc"),
        Map.entry("pin", "battery"), Map.entry("sac", "charger"), Map.entry("lop", "tire"), Map.entry("banh", "tire"),
        Map.entry("gtr", "gt-r"), Map.entry("skyline", "nissan skyline"), Map.entry("911", "porsche 911"), Map.entry("f1", "formula"));
    static final Set<String> STOP = new HashSet<>(BotController.STOP);
    static { STOP.addAll(Set.of("xe", "mo", "hinh", "duoi", "tren", "tuoi", "re", "dep", "tang", "qua", "be", "tre", "ti", "le", "ty", "scale", "k", "tr", "trieu")); }
    static final Pattern SCALE = Pattern.compile("\\b1\\s*[:/]\\s*(\\d{1,3})\\b");
    static final Pattern MAXP = Pattern.compile("(duoi|<|toi da|max)\\s*(\\d+)\\s*(k|tr|trieu)?");
    static final Pattern MINP = Pattern.compile("(tren|>|tu)\\s*(\\d+)\\s*(k|tr|trieu)");
    static final Pattern AGE = Pattern.compile("(\\d{1,2})\\s*tuoi");

    public record Parsed(String raw, String normalized, String scale, String brand, Long minPrice, Long maxPrice, Integer age,
                         List<String> terms, String corrected) {}

    static long money(String num, String unit) { long v = Long.parseLong(num); return "k".equals(unit) ? v * 1000 : unit != null ? v * 1_000_000 : v < 1000 ? v * 1_000_000 : v; }

    static String synonyms(String n) {
        String s = " " + n + " ";
        for (var e : SYN.entrySet().stream().sorted((a, b) -> b.getKey().length() - a.getKey().length()).toList())
            s = s.replace(" " + e.getKey() + " ", " " + e.getValue() + " ");
        return s.trim().replaceAll("\\s+", " ");
    }

    public static int lev(String a, String b) {
        if (Math.abs(a.length() - b.length()) > 2) return 9;
        int[] prev = new int[b.length() + 1], cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) { cur[0] = i;
            for (int j = 1; j <= b.length(); j++) cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
            int[] t = prev; prev = cur; cur = t; }
        return prev[b.length()];
    }
    static int tolerance(String w) { return w.length() >= 7 ? 2 : w.length() >= 4 ? 1 : 0; }

    /** Từ vựng sản phẩm (đã bỏ dấu) để sửa lỗi gõ. */
    private Set<String> vocab(List<Product> all) {
        Set<String> v = new HashSet<>();
        for (Product p : all) for (String w : hay(p).split(" ")) if (w.length() >= 3) v.add(w);
        return v;
    }
    static String hay(Product p) {
        return BotController.norm(p.name + " " + (p.brand == null ? "" : p.brand.name) + " " + p.type + " " + p.scale + " " + p.grp + " " + (p.sku == null ? "" : p.sku)
            + " " + ("RC".equals(p.type) ? "rc" : "") + " " + (p.specs == null ? "" : String.join(" ", p.specs.values())));
    }

    public Parsed parse(String q) { return parse(q, products.findAll()); }

    public Parsed parse(String q, List<Product> all) {
        String n = synonyms(BotController.norm(q == null ? "" : q));
        String scale = null; Matcher m = SCALE.matcher(n); if (m.find()) { scale = "1:" + m.group(1); n = m.replaceAll(" "); }
        Long maxP = null, minP = null; Integer age = null;
        m = MAXP.matcher(n); if (m.find()) { maxP = money(m.group(2), m.group(3)); n = m.replaceAll(" "); }
        m = MINP.matcher(n); if (m.find()) { minP = money(m.group(2), m.group(3)); n = m.replaceAll(" "); }
        m = AGE.matcher(n); if (m.find()) { age = Integer.parseInt(m.group(1)); n = m.replaceAll(" "); }
        Set<String> vocab = vocab(all);
        Map<String, String> brands = new HashMap<>();
        for (Product p : all) if (p.brand != null) brands.put(BotController.norm(p.brand.name), p.brand.name);
        String brand = null; String nn = " " + n + " ";
        for (var b : brands.entrySet()) if (nn.contains(" " + b.getKey() + " ")) { brand = b.getValue(); nn = nn.replace(" " + b.getKey() + " ", " "); }
        List<String> terms = new ArrayList<>(); boolean fixed = false; List<String> outWords = new ArrayList<>();
        for (String w : nn.trim().split("\\s+")) {
            if (w.isBlank() || STOP.contains(w) || (w.length() < 2 && !w.matches("\\d"))) continue;
            String t = w;
            if (!vocab.contains(w) && tolerance(w) > 0) {
                String best = null; int bd = 99;
                for (String v : vocab) { int d = lev(w, v); if (d < bd || (d == bd && best != null && v.length() < best.length())) { bd = d; best = v; } }
                if (best != null && bd <= tolerance(w)) { t = best; fixed = true; }
                else for (var b : brands.entrySet()) if (brand == null && lev(w, b.getKey().replace(" ", "")) <= tolerance(w)) { brand = b.getValue(); t = null; fixed = true; break; }
            }
            if (t != null && !terms.contains(t)) { terms.add(t); outWords.add(t); }
        }
        String corrected = fixed ? String.join(" ", (brand != null ? List.of(brand.toLowerCase()) : List.<String>of()).isEmpty() ? outWords : concat(brand.toLowerCase(), outWords)) + (scale != null ? " " + scale : "") : null;
        return new Parsed(q, n, scale, brand, minP, maxP, age, terms, corrected == null ? null : corrected.trim());
    }
    private static List<String> concat(String a, List<String> l) { List<String> r = new ArrayList<>(); r.add(a); r.addAll(l); return r; }

    /** Xếp hạng sản phẩm theo câu truy vấn. Mọi từ khoá phải khớp (AND); nếu không có kết quả thì nới thành OR. */
    public List<Product> search(String q, boolean onlyActive) { return search(q, onlyActive, false); }
    public List<Product> search(String q, boolean onlyActive, boolean strictOnly) {
        List<Product> all = products.findAll().stream().filter(p -> !onlyActive || p.active).toList();
        Parsed ps = parse(q, all);
        List<Product> base = all.stream().filter(p -> (ps.scale() == null || ps.scale().equals(p.scale))
            && (ps.brand() == null || (p.brand != null && p.brand.name.equalsIgnoreCase(ps.brand())) || hay(p).contains(BotController.norm(ps.brand())))
            && (ps.maxPrice() == null || p.price <= ps.maxPrice()) && (ps.minPrice() == null || p.price >= ps.minPrice())
            && (ps.age() == null || p.ageMin <= ps.age())).toList();
        if (ps.terms().isEmpty()) return sortBest(base, ps);
        var strict = base.stream().filter(p -> { String h = " " + hay(p) + " "; return ps.terms().stream().allMatch(t -> h.contains(t)); }).toList();
        if (!strict.isEmpty() || strictOnly) return sortBest(strict, ps);
        return base.stream().map(p -> { String h = hay(p); return Map.entry(p, ps.terms().stream().filter(h::contains).count()); })
            .filter(e -> e.getValue() > 0).sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).map(Map.Entry::getKey).toList();
    }
    private List<Product> sortBest(List<Product> l, Parsed ps) {
        return l.stream().sorted(Comparator.comparingInt((Product p) -> BotController.norm(p.name).contains(String.join(" ", ps.terms())) ? 0 : 1).thenComparing(p -> -p.sold)).toList();
    }
}
