package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.ares.shop.service.NotifyService;
import com.ares.shop.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;

/** Tính năng vận hành: hàng loạt, nhập CSV, nhật ký, phân quyền, CRM, KPI, cảnh báo yêu thích, tra cứu đơn. */
@RestController
public class OpsController {
    private final ProductRepo products; private final OrderRepo orders; private final UserRepo users; private final AuditRepo audit;
    private final SettingRepo settings; private final AlertRepo alerts; private final AdminController admin; private final OrderService orderService;
    private final BrandRepo brands; private final NotifyService notify; private final ObjectMapper json = new ObjectMapper();

    public OpsController(ProductRepo products, OrderRepo orders, UserRepo users, AuditRepo audit, SettingRepo settings, AlertRepo alerts,
                         AdminController admin, OrderService orderService, BrandRepo brands, NotifyService notify) {
        this.products = products; this.orders = orders; this.users = users; this.audit = audit; this.settings = settings; this.alerts = alerts;
        this.admin = admin; this.orderService = orderService; this.brands = brands; this.notify = notify;
    }

    // ===== Hàng loạt =====
    public record BulkStatus(List<Long> ids, String status, String note) {}
    @PostMapping("/api/admin/orders/bulk-status")
    public Map<String, Object> bulkStatus(@RequestBody BulkStatus r) {
        int ok = 0; List<String> errors = new ArrayList<>();
        for (Long id : Optional.ofNullable(r.ids()).orElse(List.of())) {
            try { admin.orderStatus(id, new AdminController.StatusReq(r.status(), r.note())); ok++; }
            catch (Exception e) { errors.add(orders.findById(id).map(o -> o.code).orElse("#" + id) + ": " + e.getMessage()); }
        }
        return Map.of("updated", ok, "errors", errors);
    }

    /** mode: SET (đặt giá = value), PERCENT (± value %), STOCK_SET, STOCK_ADD. */
    public record BulkProducts(List<Long> ids, String mode, Double value, Boolean active) {}
    @PostMapping("/api/admin/products/bulk") @Transactional
    public Map<String, Object> bulkProducts(@RequestBody BulkProducts r) {
        int n = 0; String by = Current.user().email;
        for (Long id : Optional.ofNullable(r.ids()).orElse(List.of())) {
            Product p = products.findById(id).orElse(null); if (p == null) continue;
            if (r.active() != null) p.active = r.active();
            if (r.mode() != null && r.value() != null) switch (r.mode()) {
                case "SET" -> { if (r.value() > 0) p.price = Math.round(r.value()); }
                case "PERCENT" -> p.price = Math.max(1000, Math.round(p.price * (1 + r.value() / 100) / 1000.0) * 1000);
                case "STOCK_SET", "STOCK_ADD" -> {
                    if (p.hasVariants) break;
                    int target = "STOCK_SET".equals(r.mode()) ? (int) Math.max(0, r.value()) : Math.max(0, p.stock + r.value().intValue());
                    int d = target - p.stock; p.stock = target; if (d != 0) orderService.log(p, null, d, "Sửa kho hàng loạt", by);
                }
                default -> throw ApiException.bad("Chế độ không hợp lệ");
            }
            products.save(p); n++;
        }
        return Map.of("updated", n);
    }

    @org.springframework.beans.factory.annotation.Autowired CategoryRepo categoriesRepo;
    static final List<String> IMPORT_COLS = List.of("sku", "name", "brand", "type", "grp", "scale", "price", "oldprice", "stock", "image", "description", "agemin", "active");

    /**
     * Nhập CSV có báo cáo kiểm tra từng dòng.
     * Cột: sku,name,brand,type,grp,scale,price,oldPrice,stock,image,description,ageMin,active (bắt buộc sku,name,price). Trùng SKU → cập nhật.
     * dryRun=true: chỉ kiểm tra, không ghi. atomic=true: có lỗi thì không ghi dòng nào.
     * Báo cáo: mỗi dòng {row, sku, action CREATE|UPDATE|ERROR|SKIP, errors[], warnings[], changes{field:[cũ,mới]}}.
     */
    @PostMapping(value = "/api/admin/products/import", consumes = "text/plain") @Transactional
    public Map<String, Object> importCsv(@RequestBody String csv, @RequestParam(defaultValue = "false") boolean dryRun, @RequestParam(defaultValue = "false") boolean atomic) {
        if (csv.length() > 2_000_000) throw ApiException.bad("File quá lớn (tối đa 2MB)");
        List<List<String>> rows = parseCsv(csv);
        if (rows.size() < 2) throw ApiException.bad("File CSV trống");
        List<String> head = rows.get(0).stream().map(x -> x.trim().toLowerCase()).toList();
        List<String> missing = new ArrayList<>(); for (String k : List.of("sku", "name", "price")) if (!head.contains(k)) missing.add(k);
        if (!missing.isEmpty()) throw ApiException.bad("Thiếu cột bắt buộc: " + String.join(", ", missing));
        List<String> unknown = head.stream().filter(h -> !h.isBlank() && !IMPORT_COLS.contains(h)).toList();
        Set<String> types = new HashSet<>(); categoriesRepo.findAll().forEach(c -> types.add(c.code.toUpperCase()));
        Map<String, Product> bySku = new HashMap<>(); products.findAll().forEach(p -> { if (p.sku != null) bySku.put(p.sku.toUpperCase(), p); });
        Set<String> seen = new HashSet<>();
        List<Map<String, Object>> report = new ArrayList<>(); List<Runnable> writes = new ArrayList<>();
        int created = 0, updated = 0, failed = 0, skipped = 0; String by = Current.user().email;
        for (int i = 1; i < rows.size(); i++) {
            var row = rows.get(i); if (row.stream().allMatch(String::isBlank)) continue;
            java.util.function.Function<String, String> col = k -> { int j = head.indexOf(k); return j >= 0 && j < row.size() ? row.get(j).trim() : ""; };
            List<String> errs = new ArrayList<>(), warns = new ArrayList<>(); Map<String, List<Object>> changes = new LinkedHashMap<>();
            String sku = col.apply("sku");
            if (row.size() != head.size()) warns.add("Số cột (" + row.size() + ") khác tiêu đề (" + head.size() + ")");
            if (sku.isBlank()) errs.add("Thiếu SKU"); else if (!sku.matches("[A-Za-z0-9._-]{2,40}")) errs.add("SKU chỉ gồm chữ, số, . _ - (2–40 ký tự)");
            if (!sku.isBlank() && !seen.add(sku.toUpperCase())) errs.add("SKU trùng trong file");
            Product cur = bySku.get(sku.toUpperCase()); boolean isNew = cur == null;
            String name = col.apply("name"); if (name.isBlank() && isNew) errs.add("Thiếu tên"); if (name.length() > 200) errs.add("Tên quá dài (> 200)");
            Long price = num(col.apply("price"), "Giá", errs); if (price != null && price <= 0) errs.add("Giá phải > 0"); if (price == null && isNew && col.apply("price").isBlank()) errs.add("Thiếu giá");
            Long old = num(col.apply("oldprice"), "Giá gốc", errs); if (old != null && price != null && old <= price) warns.add("Giá gốc ≤ giá bán — sẽ bỏ giá gốc");
            Long stock = num(col.apply("stock"), "Tồn kho", errs); if (stock != null && stock > 100000) errs.add("Tồn kho bất thường (> 100000)");
            Long age = num(col.apply("agemin"), "Tuổi", errs); if (age != null && age > 18) errs.add("Tuổi tối thiểu 0–18");
            String type = col.apply("type").toUpperCase(); if (!type.isBlank() && !types.contains(type)) errs.add("Danh mục '" + type + "' không tồn tại (có: " + String.join(", ", new TreeSet<>(types)) + ")");
            if (type.isBlank() && isNew) errs.add("Sản phẩm mới cần cột type");
            String scale = col.apply("scale"); if (!scale.isBlank() && !scale.matches("1:\\d{1,3}")) errs.add("Tỉ lệ phải dạng 1:N (VD 1:18)");
            String img = col.apply("image"); if (!img.isBlank() && !img.matches("(https?://|/)\\S+")) errs.add("Ảnh phải là URL http(s) hoặc đường dẫn /…");
            if (img.isBlank() && isNew) warns.add("Không có ảnh");
            String grp = col.apply("grp").toUpperCase(); if (!grp.isBlank() && !grp.matches("TOY|ACCESSORY")) errs.add("grp phải là TOY hoặc ACCESSORY");
            String act = col.apply("active").toLowerCase(); if (!act.isBlank() && !act.matches("true|false|1|0|yes|no")) errs.add("active phải true/false");
            String brand = col.apply("brand"); if (!brand.isBlank() && brands.findByName(brand).isEmpty()) warns.add("Thương hiệu mới '" + brand + "' sẽ được tạo");
            Map<String, Object> r = new LinkedHashMap<>(); r.put("row", i + 1); r.put("sku", sku); r.put("name", name.isBlank() && cur != null ? cur.name : name);
            if (!isNew) {
                if (!name.isBlank() && !name.equals(cur.name)) changes.put("name", List.of(cur.name, name));
                if (price != null && price != cur.price) changes.put("price", List.of(cur.price, price));
                if (stock != null && stock != cur.stock) changes.put("stock", List.of(cur.stock, stock));
                if (!scale.isBlank() && !scale.equals(cur.scale)) changes.put("scale", List.of(String.valueOf(cur.scale), scale));
                if (!type.isBlank() && !type.equals(cur.type)) changes.put("type", List.of(String.valueOf(cur.type), type));
                if (price != null && cur.price > 0 && Math.abs(price - cur.price) * 2 > cur.price) warns.add("Giá đổi > 50% so với hiện tại");
                if (cur.hasVariants && stock != null) warns.add("Sản phẩm có biến thể — bỏ qua tồn kho");
            }
            r.put("errors", errs); r.put("warnings", warns); r.put("changes", changes);
            if (!errs.isEmpty()) { r.put("action", "ERROR"); failed++; report.add(r); continue; }
            if (!isNew && changes.isEmpty() && img.isBlank() && col.apply("description").isBlank() && old == null && act.isBlank() && brand.isBlank()) { r.put("action", "SKIP"); skipped++; report.add(r); continue; }
            r.put("action", isNew ? "CREATE" : "UPDATE"); if (isNew) created++; else updated++; report.add(r);
            final Long fPrice = price, fOld = old, fStock = stock, fAge = age;
            writes.add(() -> {
                Product p = isNew ? new Product() : cur; if (isNew) { p.sku = sku; p.isNew = true; }
                if (!name.isBlank()) p.name = name;
                if (!brand.isBlank()) p.brand = brands.findByName(brand).orElseGet(() -> { Brand b = new Brand(); b.name = brand; return brands.save(b); });
                if (!type.isBlank()) { p.type = type; categoriesRepo.findByCode(type).ifPresent(c -> p.grp = c.grp); }
                if (!grp.isBlank()) p.grp = grp;
                if (!scale.isBlank()) p.scale = scale;
                if (fPrice != null) p.price = fPrice;
                if (fOld != null) p.oldPrice = fOld > p.price ? fOld : null;
                if (fStock != null && !p.hasVariants) { int d = (int) (fStock - p.stock); p.stock = fStock.intValue(); if (!isNew && d != 0) orderService.log(p, null, d, "Nhập CSV", by); }
                if (fAge != null) p.ageMin = fAge.intValue();
                if (!img.isBlank()) { p.images.clear(); p.images.add(img); }
                if (!col.apply("description").isBlank()) p.description = col.apply("description");
                if (!act.isBlank()) p.active = act.matches("true|1|yes");
                products.save(p);
            });
        }
        boolean blocked = atomic && failed > 0;
        if (!dryRun && !blocked) writes.forEach(Runnable::run);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("dryRun", dryRun); out.put("applied", !dryRun && !blocked); out.put("blockedByErrors", blocked);
        out.put("created", created); out.put("updated", updated); out.put("skipped", skipped); out.put("failed", failed);
        out.put("unknownColumns", unknown); out.put("report", report);
        out.put("errors", report.stream().filter(x -> "ERROR".equals(x.get("action"))).map(x -> "Dòng " + x.get("row") + ": " + String.join("; ", (List<String>) x.get("errors"))).toList());
        return out;
    }
    private static Long num(String v, String label, List<String> errs) {
        if (v == null || v.isBlank()) return null;
        String c = v.replaceAll("[\\s.,đ₫]", "");
        if (!c.matches("\\d{1,12}")) { errs.add(label + " không phải số: '" + v + "'"); return null; }
        return Long.parseLong(c);
    }

    /** Xuất toàn bộ sản phẩm ra CSV (cùng định dạng nhập — sửa rồi nhập lại được). */
    @GetMapping(value = "/api/admin/products/export.csv", produces = "text/csv;charset=UTF-8")
    public String exportCsv() {
        StringBuilder sb = new StringBuilder("\uFEFFsku,name,brand,type,grp,scale,price,oldPrice,stock,image,description,ageMin,active\n");
        for (Product p : products.findAll(org.springframework.data.domain.Sort.by("id")))
            sb.append(String.join(",", Arrays.asList(q(p.sku), q(p.name), q(p.brand == null ? "" : p.brand.name), q(p.type), q(p.grp), q(p.scale), String.valueOf(p.price),
                p.oldPrice == null ? "" : String.valueOf(p.oldPrice), String.valueOf(p.stock), q(p.images.isEmpty() ? "" : p.images.get(0)), q(p.description), String.valueOf(p.ageMin), String.valueOf(p.active)))).append('\n');
        return sb.toString();
    }
    private static String q(String s) { if (s == null) return ""; return s.matches(".*[,\"\n\r].*") ? "\"" + s.replace("\"", "\"\"") + "\"" : s; }

    static List<List<String>> parseCsv(String s) {
        List<List<String>> out = new ArrayList<>(); List<String> row = new ArrayList<>(); StringBuilder f = new StringBuilder(); boolean q = false;
        s = s.replace("\uFEFF", "");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (q) { if (c == '"') { if (i + 1 < s.length() && s.charAt(i + 1) == '"') { f.append('"'); i++; } else q = false; } else f.append(c); }
            else if (c == '"') q = true; else if (c == ',') { row.add(f.toString()); f.setLength(0); }
            else if (c == '\n' || c == '\r') { if (c == '\r' && i + 1 < s.length() && s.charAt(i + 1) == '\n') i++; row.add(f.toString()); f.setLength(0); out.add(row); row = new ArrayList<>(); }
            else f.append(c);
        }
        if (f.length() > 0 || !row.isEmpty()) { row.add(f.toString()); out.add(row); }
        return out;
    }

    // ===== Nhật ký =====
    @GetMapping("/api/admin/audit")
    public List<AuditLog> auditLog() { return audit.findTop300ByOrderByIdDesc(); }

    // ===== Ma trận phân quyền nhân viên =====
    static final List<String> SECTIONS = List.of("products", "orders", "customers", "inventory", "returns", "promotions", "flash-sales", "banners", "catalog", "auctions", "questions", "reviews", "tickets", "chat", "purchasing", "carts", "segments", "bundles", "home-layout", "emails", "analytics", "chatbot", "preorders", "collections", "broadcast");
    @GetMapping("/api/admin/permissions")
    public Map<String, Object> perms() throws Exception {
        var s = settings.findById("staffPerms").orElse(null);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sections", SECTIONS);
        m.put("staff", s == null ? SECTIONS : json.readValue(s.v, List.class));
        return m;
    }
    @PutMapping("/api/admin/permissions")
    public Map<String, Object> savePerms(@RequestBody Map<String, List<String>> body) throws Exception {
        if (Current.user().role != Enums.Role.ADMIN) throw ApiException.bad("Chỉ quản trị viên được sửa phân quyền");
        AppSetting s = new AppSetting(); s.k = "staffPerms";
        s.v = json.writeValueAsString(Optional.ofNullable(body.get("staff")).orElse(List.of()).stream().filter(SECTIONS::contains).toList());
        settings.save(s); return perms();
    }

    // ===== CRM: ghi chú + nhãn khách =====
    @GetMapping("/api/admin/customers/{id}/crm")
    public Map<String, Object> crm(@PathVariable Long id) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy khách"));
        return Map.of("note", Optional.ofNullable(u.staffNote).orElse(""), "tags", u.tags);
    }
    public record Crm(String note, List<String> tags) {}
    @PutMapping("/api/admin/customers/{id}/crm") @Transactional
    public Map<String, Object> saveCrm(@PathVariable Long id, @RequestBody Crm r) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy khách"));
        if (r.note() != null) u.staffNote = r.note().length() > 2000 ? r.note().substring(0, 2000) : r.note();
        if (r.tags() != null) { u.tags.clear(); r.tags().stream().map(String::trim).filter(t -> !t.isBlank()).limit(10).forEach(u.tags::add); }
        users.save(u); return crm(id);
    }
    @GetMapping("/api/admin/customer-tags")
    public Map<Long, Set<String>> allTags() {
        Map<Long, Set<String>> m = new HashMap<>(); users.findAll().forEach(u -> { if (!u.tags.isEmpty()) m.put(u.id, u.tags); }); return m;
    }

    // ===== KPI theo khoảng ngày =====
    @GetMapping("/api/admin/kpi")
    public Map<String, Object> kpi(@RequestParam String from, @RequestParam String to) {
        LocalDateTime a = LocalDate.parse(from).atStartOfDay(), b = LocalDate.parse(to).plusDays(1).atStartOfDay();
        long days = Math.max(1, Duration.between(a, b).toDays());
        var all = orders.findAll();
        var cur = all.stream().filter(o -> !o.createdAt.isBefore(a) && o.createdAt.isBefore(b)).toList();
        var prev = all.stream().filter(o -> !o.createdAt.isBefore(a.minusDays(days)) && o.createdAt.isBefore(a)).toList();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("current", summary(cur, a, b)); m.put("previous", summary(prev, a.minusDays(days), a));
        Map<String, Long> daily = new LinkedHashMap<>();
        for (LocalDate d = a.toLocalDate(); d.isBefore(b.toLocalDate()); d = d.plusDays(1)) daily.put(d.toString(), 0L);
        cur.stream().filter(AdminController::counts).forEach(o -> daily.merge(o.createdAt.toLocalDate().toString(), o.total, Long::sum));
        m.put("daily", daily);
        return m;
    }
    private Map<String, Object> summary(List<CustomerOrder> list, LocalDateTime a, LocalDateTime b) {
        var ok = list.stream().filter(AdminController::counts).toList();
        long revenue = ok.stream().mapToLong(o -> o.total).sum();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("revenue", revenue); m.put("orders", list.size()); m.put("aov", ok.isEmpty() ? 0 : revenue / ok.size());
        m.put("items", ok.stream().flatMap(o -> o.items.stream()).mapToLong(i -> i.quantity).sum());
        m.put("cancelRate", list.isEmpty() ? 0 : Math.round(list.stream().filter(o -> o.status == Enums.OrderStatus.CANCELLED).count() * 1000.0 / list.size()) / 10.0);
        m.put("newCustomers", users.findAll().stream().filter(u -> u.createdAt != null && !u.createdAt.isBefore(a) && u.createdAt.isBefore(b)).count());
        return m;
    }

    // ===== Cảnh báo giảm giá / có hàng lại (khách) =====
    @GetMapping("/api/alerts")
    public List<WatchAlert> myAlerts() { return alerts.findByUserId(Current.user().id).stream().filter(x -> !Boolean.TRUE.equals(x.fired)).toList(); }
    public record AlertReq(Long productId, String kind) {}
    @PostMapping("/api/alerts")
    public WatchAlert addAlert(@RequestBody AlertReq r) {
        User u = Current.user();
        if (!"PRICE".equals(r.kind()) && !"STOCK".equals(r.kind())) throw ApiException.bad("Loại cảnh báo không hợp lệ");
        Product p = products.findById(r.productId()).orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm"));
        var dup = alerts.findByUserId(u.id).stream().filter(x -> x.productId.equals(p.id) && x.kind.equals(r.kind()) && !Boolean.TRUE.equals(x.fired)).findFirst();
        if (dup.isPresent()) return dup.get();
        WatchAlert w = new WatchAlert(); w.user = u; w.productId = p.id; w.kind = r.kind(); w.basePrice = p.price; w.fired = false;
        return alerts.save(w);
    }
    @DeleteMapping("/api/alerts/{id}")
    public void delAlert(@PathVariable Long id) {
        alerts.findById(id).filter(x -> x.user.id.equals(Current.user().id)).ifPresent(alerts::delete);
    }

    // ===== Tra cứu trạng thái đơn (chatbot, không cần đăng nhập: mã đơn + SĐT) =====
    @GetMapping("/api/public/order-status")
    public Map<String, Object> orderStatus(@RequestParam String code, @RequestParam String phone) {
        String digits = phone.replaceAll("\\D", "");
        CustomerOrder o = orders.findByCode(code.trim().toUpperCase())
                .filter(x -> x.phone != null && x.phone.replaceAll("\\D", "").endsWith(digits.length() >= 4 ? digits.substring(digits.length() - 4) : "\u0000") && digits.length() >= 9)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn khớp mã và số điện thoại"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", o.code); m.put("status", o.status); m.put("paymentStatus", o.paymentStatus); m.put("total", o.total);
        m.put("createdAt", o.createdAt); m.put("timeline", o.timeline); m.put("items", o.items.size());
        return m;
    }

    // ===== Phương thức thanh toán ưa thích =====
    @PutMapping("/api/account/payment-preference")
    public Map<String, String> setPay(@RequestBody Map<String, String> b) {
        User u = users.findById(Current.user().id).orElseThrow();
        String v = b.get("method"); if (v != null && !v.isBlank()) Enums.PaymentMethod.valueOf(v);
        u.preferredPayment = v == null || v.isBlank() ? null : v; users.save(u);
        return Map.of("method", Optional.ofNullable(u.preferredPayment).orElse(""));
    }
}
