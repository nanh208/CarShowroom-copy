package com.ares.shop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import java.text.Normalizer;
import java.util.*;

/**
 * Phân trang / tìm kiếm / sắp xếp / lọc phía máy chủ cho MỌI danh sách quản trị (GET /api/admin/** trả về List).
 * Kích hoạt khi request có tham số {@code page}: q=từ khoá (bỏ dấu, tìm trên mọi trường chữ, kể cả lồng nhau),
 * sort=field[,desc] (hỗ trợ đường dẫn a.b), f.field=giá trị (lọc chính xác, nhiều giá trị cách bằng dấu phẩy), size ≤ 200.
 * Không có {@code page} → trả nguyên List như cũ (tương thích ngược).
 */
@RestControllerAdvice
public class AdminPagingAdvice implements ResponseBodyAdvice<Object> {
    private final ObjectMapper om;
    public AdminPagingAdvice(ObjectMapper om) { this.om = om; }

    @Override public boolean supports(MethodParameter rt, Class<? extends HttpMessageConverter<?>> c) { return List.class.isAssignableFrom(rt.getParameterType()); }

    static String norm(Object s) { return s == null ? "" : Normalizer.normalize(String.valueOf(s).toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace('đ', 'd'); }
    static void text(Object v, StringBuilder sb, int depth) {
        if (v == null || depth > 3) return;
        if (v instanceof Map<?, ?> m) m.values().forEach(x -> text(x, sb, depth + 1));
        else if (v instanceof Collection<?> c) { int i = 0; for (Object x : c) { if (i++ > 20) break; text(x, sb, depth + 1); } }
        else sb.append(v).append(' ');
    }
    static Object path(Object row, String p) {
        Object cur = row;
        for (String k : p.split("\\.")) { if (!(cur instanceof Map<?, ?> m)) return null; cur = m.get(k); }
        return cur;
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    static int cmp(Object a, Object b) {
        if (a == null) return b == null ? 0 : 1; if (b == null) return -1;
        if (a instanceof Number x && b instanceof Number y) return Double.compare(x.doubleValue(), y.doubleValue());
        if (a instanceof Comparable x && a.getClass() == b.getClass()) return x.compareTo(b);
        return norm(a).compareTo(norm(b));
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter rt, MediaType mt, Class<? extends HttpMessageConverter<?>> c, ServerHttpRequest req, ServerHttpResponse res) {
        if (!(body instanceof List<?> list) || !(req instanceof ServletServerHttpRequest sr)) return body;
        var r = sr.getServletRequest();
        if (!"GET".equals(r.getMethod()) || !r.getRequestURI().startsWith("/api/admin/") || r.getParameter("page") == null) return body;
        int page = Math.max(0, parse(r.getParameter("page"), 0)), size = Math.max(1, Math.min(200, parse(r.getParameter("size"), 20)));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object o : list) rows.add(om.convertValue(o, Map.class));
        String q = norm(Optional.ofNullable(r.getParameter("q")).orElse("").trim());
        if (!q.isEmpty()) {
            String[] words = q.split("\\s+");
            rows.removeIf(row -> { StringBuilder sb = new StringBuilder(); text(row, sb, 0); String h = norm(sb); for (String w : words) if (!h.contains(w)) return true; return false; });
        }
        for (var e : r.getParameterMap().entrySet()) {
            if (!e.getKey().startsWith("f.") || e.getValue()[0].isBlank()) continue;
            Set<String> want = new HashSet<>(Arrays.asList(norm(e.getValue()[0]).split(",")));
            String f = e.getKey().substring(2);
            rows.removeIf(row -> { Object v = path(row, f); if (v instanceof Collection<?> col) { for (Object x : col) if (want.contains(norm(x))) return false; return true; } return !want.contains(norm(v)); });
        }
        String s = r.getParameter("sort");
        if (s != null && !s.isBlank()) {
            String[] sp = s.split(","); boolean desc = sp.length > 1 && sp[1].equalsIgnoreCase("desc");
            Comparator<Map<String, Object>> cm = (a, b) -> cmp(path(a, sp[0]), path(b, sp[0]));
            rows.sort(desc ? (a, b) -> { Object x = path(a, sp[0]), y = path(b, sp[0]); if (x == null || y == null) return cm.compare(a, b); return -cmp(x, y); } : cm);
        }
        int total = rows.size(), from = Math.min(page * size, total);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("content", rows.subList(from, Math.min(from + size, total))); out.put("totalElements", total);
        out.put("totalPages", Math.max(1, (total + size - 1) / size)); out.put("page", page); out.put("size", size);
        return out;
    }
    static int parse(String s, int d) { try { return Integer.parseInt(s); } catch (Exception e) { return d; } }
}
