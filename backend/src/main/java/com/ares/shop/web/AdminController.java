package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.ares.shop.service.NotifyService;
import com.ares.shop.service.OrderService;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController @RequestMapping("/api/admin")
public class AdminController {
    @org.springframework.beans.factory.annotation.Autowired org.springframework.context.ApplicationEventPublisher events;
    private final ProductRepo products; private final BrandRepo brands; private final OrderRepo orders; private final UserRepo users;
    private final NotifyService notify; private final PromotionRepo promos; private final OrderService orderService;
    private final CategoryRepo categories; private final VariantRepo variants; private final FlashSaleRepo flash; private final BannerRepo banners;
    private final StockRepo stock; private final ReturnRepo returns; private final TicketRepo tickets; private final QuestionRepo questions;

    public AdminController(ProductRepo products, BrandRepo brands, OrderRepo orders, UserRepo users, PromotionRepo promos, OrderService orderService,
                           NotifyService notify, CategoryRepo categories, VariantRepo variants, FlashSaleRepo flash, BannerRepo banners,
                           StockRepo stock, ReturnRepo returns, TicketRepo tickets, QuestionRepo questions) {
        this.products = products; this.brands = brands; this.orders = orders; this.users = users; this.promos = promos; this.orderService = orderService;
        this.notify = notify; this.categories = categories; this.variants = variants; this.flash = flash; this.banners = banners;
        this.stock = stock; this.returns = returns; this.tickets = tickets; this.questions = questions;
    }

    static boolean counts(CustomerOrder o) { return o.status != Enums.OrderStatus.CANCELLED && o.status != Enums.OrderStatus.RETURNED; }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        var all = orders.findAll();
        long revenue = all.stream().filter(AdminController::counts).mapToLong(o -> o.paidAmount).sum();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (var s : Enums.OrderStatus.values()) byStatus.put(s.name(), all.stream().filter(o -> o.status == s).count());
        Map<Long, long[]> agg = new HashMap<>();
        all.stream().filter(AdminController::counts).flatMap(o -> o.items.stream()).filter(i -> i.productId != null)
                .forEach(i -> { long[] a = agg.computeIfAbsent(i.productId, k -> new long[2]); a[0] += i.quantity; a[1] += i.unitPrice * i.quantity; });
        var top = agg.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue()[1], a.getValue()[1])).limit(5)
                .map(e -> products.findById(e.getKey()).map(p -> Map.<String, Object>of("id", p.id, "name", p.name, "image", p.images.isEmpty() ? "" : p.images.get(0),
                        "qty", e.getValue()[0], "revenue", e.getValue()[1])).orElse(null)).filter(Objects::nonNull).toList();
        if (top.isEmpty()) top = products.findTop8ByActiveTrueOrderBySoldDesc().stream().limit(5)
                .map(p -> Map.<String, Object>of("id", p.id, "name", p.name, "image", p.images.isEmpty() ? "" : p.images.get(0), "qty", p.sold, "revenue", p.sold * p.price)).toList();
        var low = products.findByActiveTrueAndStockLessThanEqualOrderByStockAsc(5);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("revenue", revenue); m.put("orders", all.size()); m.put("pendingOrders", byStatus.get("PENDING")); m.put("customers", users.count());
        m.put("products", products.count()); m.put("lowStock", low.size()); m.put("lowStockItems", low.stream().limit(8).toList());
        m.put("returnsPending", returns.findAll().stream().filter(r -> r.status == Enums.ReturnStatus.PENDING).count());
        m.put("ticketsOpen", tickets.findAll().stream().filter(t -> t.status == Enums.TicketStatus.OPEN).count());
        m.put("questionsOpen", questions.findAll().stream().filter(q -> q.answer == null).count());
        m.put("ordersByStatus", byStatus); m.put("topProducts", top);
        m.put("recentOrders", orders.findAllByOrderByCreatedAtDesc().stream().limit(6).toList());
        return m;
    }

    // ===== Sản phẩm + biến thể =====
    @GetMapping("/products")
    public Page<Product> products(@RequestParam(required = false) String q, @RequestParam(required = false) Long brandId,
                                  @RequestParam(required = false) String type, @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return products.findAll(CatalogController.filter(new CatalogController.Filter(q, brandId, type, null, null, null, null, null, null, null), false),
                PageRequest.of(page, size, Sort.by("id").descending()));
    }

    @GetMapping("/products/{id}")
    public Map<String, Object> product(@PathVariable Long id) {
        return Map.of("product", products.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm")),
                "variants", variants.findByProductIdOrderByIdAsc(id));
    }

    public record ProductReq(Product product, List<ProductVariant> variants) {}

    @PostMapping("/products") @Transactional
    public Product createProduct(@RequestBody ProductReq r) { Product p = r.product(); p.id = null; return saveProduct(p, r.variants()); }

    @PutMapping("/products/{id}") @Transactional
    public Product updateProduct(@PathVariable Long id, @RequestBody ProductReq r) {
        Product cur = products.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm"));
        Product p = r.product(); p.id = id; p.createdAt = cur.createdAt; p.sold = cur.sold; p.rating = cur.rating; p.reviewCount = cur.reviewCount;
        if (p.stock != cur.stock && (r.variants() == null || r.variants().isEmpty())) orderService.log(cur, null, p.stock - cur.stock, "Sửa sản phẩm", Current.user().email);
        return saveProduct(p, r.variants());
    }

    private Product saveProduct(Product p, List<ProductVariant> vs) {
        if (p.name == null || p.name.isBlank()) throw ApiException.bad("Tên sản phẩm bắt buộc");
        if (p.price <= 0) throw ApiException.bad("Giá phải lớn hơn 0");
        if (p.brand != null && p.brand.id != null) p.brand = brands.findById(p.brand.id).orElse(null); else p.brand = null;
        categories.findByCode(p.type).ifPresentOrElse(c -> p.grp = c.grp, () -> { throw ApiException.bad("Danh mục không hợp lệ"); });
        if (p.sku == null || p.sku.isBlank()) p.sku = null;
        p.images.removeIf(s -> s == null || s.isBlank());
        if (p.model3d != null && p.model3d.isBlank()) p.model3d = null;
        if (p.model3d != null && !(p.model3d.matches("^(/|https://)[^\\s\"'<>]+\\.(glb|gltf)(\\?.*)?$"))) throw ApiException.bad("Mô hình 3D phải là URL .glb/.gltf (bắt đầu bằng / hoặc https://)");
        Product saved = products.save(p);
        if (vs != null) {
            var keep = new HashSet<Long>();
            for (ProductVariant v : vs) {
                if (v.name == null || v.name.isBlank()) continue;
                ProductVariant cur = v.id == null ? new ProductVariant() : variants.findById(v.id).filter(x -> x.product.id.equals(saved.id)).orElse(new ProductVariant());
                if (cur.id != null && cur.stock != v.stock) orderService.log(saved, cur, v.stock - cur.stock, "Sửa biến thể", Current.user().email);
                cur.product = saved; cur.name = v.name; cur.color = v.color; cur.price = v.price > 0 ? v.price : saved.price; cur.oldPrice = v.oldPrice;
                cur.stock = v.stock; cur.image = v.image; cur.active = true;
                keep.add(variants.save(cur).id);
            }
            for (ProductVariant old : variants.findByProductIdOrderByIdAsc(saved.id)) if (!keep.contains(old.id)) { old.active = false; variants.save(old); }
            var active = variants.findByProductIdOrderByIdAsc(saved.id).stream().filter(v -> v.active).toList();
            saved.hasVariants = !active.isEmpty();
            if (saved.hasVariants) {
                saved.stock = active.stream().mapToInt(v -> v.stock).sum();
                saved.price = active.stream().mapToLong(v -> v.price).min().orElse(saved.price);
            }
        }
        return products.save(saved);
    }

    @DeleteMapping("/products/{id}")
    public void deleteProduct(@PathVariable Long id) { products.findById(id).ifPresent(p -> { p.active = false; products.save(p); }); }

    @PostMapping("/products/{id}/restore")
    public Product restore(@PathVariable Long id) { Product p = products.findById(id).orElseThrow(); p.active = true; return products.save(p); }

    // ===== Kho =====
    public record AdjustReq(Long productId, Long variantId, int delta, String reason) {}

    @PostMapping("/inventory/adjust") @Transactional
    public Map<String, Object> adjust(@RequestBody AdjustReq r) {
        Product p = products.findById(r.productId()).orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm"));
        if (r.delta() == 0) throw ApiException.bad("Số lượng điều chỉnh khác 0");
        ProductVariant v = null;
        if (r.variantId() != null) {
            v = variants.findById(r.variantId()).filter(x -> x.product.id.equals(p.id)).orElseThrow(() -> ApiException.bad("Biến thể không hợp lệ"));
            if (v.stock + r.delta() < 0) throw ApiException.bad("Tồn kho không thể âm");
            v.stock += r.delta(); variants.save(v);
        } else if (p.hasVariants) throw ApiException.bad("Sản phẩm có biến thể — chọn biến thể để điều chỉnh");
        if (p.stock + r.delta() < 0) throw ApiException.bad("Tồn kho không thể âm");
        p.stock += r.delta(); products.save(p);
        orderService.log(p, v, r.delta(), r.reason() == null || r.reason().isBlank() ? "Điều chỉnh kho" : r.reason(), Current.user().email);
        return Map.of("stock", p.stock, "variantStock", v == null ? -1 : v.stock);
    }

    @GetMapping("/inventory/movements")
    public List<StockMovement> movements() { return stock.findTop200ByOrderByCreatedAtDesc(); }

    @GetMapping("/inventory/variants")
    public List<Map<String, Object>> allVariants() {
        return variants.findAll().stream().filter(v -> v.active).map(v -> Map.<String, Object>of("id", v.id, "productId", v.product.id, "name", v.name, "stock", v.stock)).toList();
    }

    // ===== Danh mục & thương hiệu =====
    @PostMapping("/categories") public Category createCat(@RequestBody Category c) { c.id = null; c.code = c.code.trim().toUpperCase(); return categories.save(c); }
    @PutMapping("/categories/{id}") public Category updateCat(@PathVariable Long id, @RequestBody Category c) { c.id = id; return categories.save(c); }
    @DeleteMapping("/categories/{id}") public void deleteCat(@PathVariable Long id) {
        Category c = categories.findById(id).orElseThrow();
        if (!products.findAll(CatalogController.filter(new CatalogController.Filter(null, null, c.code, null, null, null, null, null, null, null), false)).isEmpty())
            throw ApiException.bad("Danh mục đang có sản phẩm, không thể xoá");
        categories.delete(c);
    }

    @PostMapping("/brands") public Brand createBrand(@RequestBody Brand b) { b.id = null; return brands.save(b); }
    @PutMapping("/brands/{id}") public Brand updateBrand(@PathVariable Long id, @RequestBody Brand b) { b.id = id; return brands.save(b); }
    @DeleteMapping("/brands/{id}") public void deleteBrand(@PathVariable Long id) {
        if (!products.findAll(CatalogController.filter(new CatalogController.Filter(null, id, null, null, null, null, null, null, null, null), false)).isEmpty())
            throw ApiException.bad("Thương hiệu đang có sản phẩm, không thể xoá");
        brands.deleteById(id);
    }

    // ===== Đơn hàng =====
    @GetMapping("/orders")
    public List<CustomerOrder> orders(@RequestParam(required = false) String status, @RequestParam(required = false) String q) {
        return orders.findAllByOrderByCreatedAtDesc().stream().filter(o -> status == null || status.isBlank() || o.status.name().equals(status))
                .filter(o -> q == null || q.isBlank() || (o.code + " " + o.buyerName + " " + o.phone).toLowerCase().contains(q.toLowerCase())).toList();
    }

    public record StatusReq(String status, String note) {}

    /** Quy trình: PENDING → CONFIRMED → SHIPPING → COMPLETED; huỷ khi PENDING/CONFIRMED. */
    static final Map<Enums.OrderStatus, Set<Enums.OrderStatus>> FLOW = Map.of(
            Enums.OrderStatus.PENDING, Set.of(Enums.OrderStatus.CONFIRMED, Enums.OrderStatus.CANCELLED),
            Enums.OrderStatus.CONFIRMED, Set.of(Enums.OrderStatus.SHIPPING, Enums.OrderStatus.CANCELLED),
            Enums.OrderStatus.SHIPPING, Set.of(Enums.OrderStatus.COMPLETED),
            Enums.OrderStatus.COMPLETED, Set.of(), Enums.OrderStatus.CANCELLED, Set.of(), Enums.OrderStatus.RETURNED, Set.of());

    @PatchMapping("/orders/{id}/status") @Transactional
    public CustomerOrder orderStatus(@PathVariable Long id, @RequestBody StatusReq r) {
        CustomerOrder o = orders.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn"));
        var s = Enums.OrderStatus.valueOf(r.status());
        if (!FLOW.get(o.status).contains(s)) throw ApiException.bad("Không thể chuyển từ " + o.status + " sang " + s);
        com.ares.shop.service.FraudService.requireCleared(o, s);
        String from = o.status.name();
        if (s == Enums.OrderStatus.CANCELLED) { var c = orderService.cancel(o, r.note() == null ? "Shop huỷ" : r.note()); events.publishEvent(new com.ares.shop.service.OrderEvents.OrderStatusChanged(c, from, "CANCELLED")); return c; }
        o.status = s;
        o.timeline.put(s.name(), LocalDateTime.now());
        String label = Map.of("CONFIRMED", "Đã xác nhận, đang đóng gói", "SHIPPING", "Đã giao cho đơn vị vận chuyển", "COMPLETED", "Giao hàng thành công").get(s.name());
        if (s == Enums.OrderStatus.COMPLETED) orderService.complete(o);
        notify.send(o.user, "ORDER", "Cập nhật đơn " + o.code, label + ".", "/orders/" + o.code);
        CustomerOrder saved = orders.save(o);
        events.publishEvent(new com.ares.shop.service.OrderEvents.OrderStatusChanged(saved, from, s.name()));
        if (s == Enums.OrderStatus.COMPLETED && o.user != null) { User u = users.findById(o.user.id).orElseThrow(); orderService.updateTier(u); users.save(u); }
        return saved;
    }

    @PostMapping("/orders/{id}/mark-paid")
    public CustomerOrder markPaid(@PathVariable Long id) { return orderService.pay(orders.findById(id).orElseThrow()); }

    // ===== Đổi trả / hoàn tiền =====
    @GetMapping("/returns") public List<ReturnRequest> returns() { return returns.findAllByOrderByCreatedAtDesc(); }

    @PatchMapping("/returns/{id}") @Transactional
    public ReturnRequest handleReturn(@PathVariable Long id, @RequestBody StatusReq r) {
        ReturnRequest x = returns.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy yêu cầu"));
        var s = Enums.ReturnStatus.valueOf(r.status());
        CustomerOrder o = x.order;
        if (s == Enums.ReturnStatus.REFUNDED) {
            if (x.status != Enums.ReturnStatus.APPROVED) throw ApiException.bad("Cần duyệt yêu cầu trước khi hoàn tiền");
            orderService.restock(o, "Trả hàng " + o.code);
            o.status = Enums.OrderStatus.RETURNED; o.paymentStatus = Enums.PaymentStatus.REFUNDED; o.timeline.put("RETURNED", LocalDateTime.now());
            if (o.pointsEarned > 0 && o.user != null) { User u = users.findById(o.user.id).orElseThrow(); u.points = Math.max(0, u.points - o.pointsEarned); users.save(u); orderService.pointTx(u, -o.pointsEarned, "Thu hồi điểm đơn trả " + o.code); }
            orders.save(o);
        }
        x.status = s; x.adminNote = r.note(); x.updatedAt = LocalDateTime.now();
        String msg = switch (s) { case APPROVED -> "Yêu cầu được duyệt. Vui lòng gửi hàng về shop, phí vận chuyển do ARES chịu.";
            case REJECTED -> "Yêu cầu bị từ chối" + (r.note() == null ? "." : ": " + r.note());
            case REFUNDED -> "Đã hoàn " + String.format("%,d", x.refundAmount).replace(',', '.') + "đ về phương thức thanh toán ban đầu.";
            default -> "Đang xử lý."; };
        notify.send(x.user, "ORDER", "Đổi trả đơn " + x.orderCode, msg, "/orders/" + x.orderCode);
        return returns.save(x);
    }

    // ===== Khách hàng =====
    @GetMapping("/customers")
    public List<Map<String, Object>> customers() {
        var all = orders.findAll();
        return users.findAll(Sort.by("id").descending()).stream().map(u -> {
            var mine = all.stream().filter(o -> o.user != null && o.user.id.equals(u.id) && counts(o)).toList();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("user", u); m.put("tags", new ArrayList<>(u.tags)); m.put("role", String.valueOf(u.role)); m.put("status", u.locked ? "LOCKED" : "ACTIVE"); m.put("orderCount", mine.size()); m.put("totalSpent", mine.stream().mapToLong(o -> o.total).sum());
            return m;
        }).toList();
    }

    public record UserPatch(Boolean locked, Integer pointsDelta, String reason) {}

    @PatchMapping("/customers/{id}")
    public User patchUser(@PathVariable Long id, @RequestBody UserPatch r) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy khách hàng"));
        if (u.id.equals(Current.user().id)) throw ApiException.bad("Không thể tự thay đổi tài khoản của mình");
        if (r.locked() != null) {
            if (u.role == Enums.Role.ADMIN && Current.user().role != Enums.Role.ADMIN) throw ApiException.bad("Không có quyền");
            u.locked = r.locked();
        }
        if (r.pointsDelta() != null && r.pointsDelta() != 0) {
            u.points = Math.max(0, u.points + r.pointsDelta());
            orderService.pointTx(u, r.pointsDelta(), r.reason() == null ? "Điều chỉnh bởi shop" : r.reason());
            notify.send(u, "SYSTEM", (r.pointsDelta() > 0 ? "Bạn được cộng " : "Bạn bị trừ ") + Math.abs(r.pointsDelta()) + " điểm", r.reason() == null ? "" : r.reason(), "/account?tab=points");
        }
        return users.save(u);
    }

    // ===== Nhân viên (chỉ ADMIN) =====
    @PatchMapping("/staff/{id}/role")
    public User role(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User u = users.findById(id).orElseThrow();
        if (u.id.equals(Current.user().id)) throw ApiException.bad("Không thể tự đổi quyền");
        u.role = Enums.Role.valueOf(body.get("role")); return users.save(u);
    }

    // ===== Voucher =====
    @GetMapping("/promotions") public List<Promotion> promos() { return promos.findAll(Sort.by("id").descending()); }
    @PostMapping("/promotions") public Promotion createPromo(@RequestBody Promotion p) { p.id = null; return promos.save(checkPromo(p)); }
    @PutMapping("/promotions/{id}") public Promotion updatePromo(@PathVariable Long id, @RequestBody Promotion p) { p.id = id; return promos.save(checkPromo(p)); }
    @DeleteMapping("/promotions/{id}") public void deletePromo(@PathVariable Long id) { promos.deleteById(id); }

    private Promotion checkPromo(Promotion p) {
        if (p.code == null || p.code.isBlank()) throw ApiException.bad("Mã voucher bắt buộc");
        if (p.kind == null) p.kind = Enums.VoucherKind.PERCENT;
        if (p.kind == Enums.VoucherKind.PERCENT && (p.discountPercent <= 0 || p.discountPercent > 100)) throw ApiException.bad("% giảm phải từ 1-100");
        if (p.kind == Enums.VoucherKind.FIXED && p.amount <= 0) throw ApiException.bad("Số tiền giảm phải lớn hơn 0");
        p.code = p.code.trim().toUpperCase();
        return p;
    }

    // ===== Flash sale =====
    @GetMapping("/flash-sales") public List<FlashSale> flashSales() { return flash.findAllByOrderByStartTimeDesc(); }
    @PostMapping("/flash-sales") public FlashSale createFlash(@RequestBody FlashSale f) { f.id = null; return flash.save(checkFlash(f)); }
    @PutMapping("/flash-sales/{id}") public FlashSale updateFlash(@PathVariable Long id, @RequestBody FlashSale f) { f.id = id; return flash.save(checkFlash(f)); }
    @DeleteMapping("/flash-sales/{id}") public void deleteFlash(@PathVariable Long id) { flash.deleteById(id); }

    private FlashSale checkFlash(FlashSale f) {
        Product p = products.findById(f.productId == null ? -1 : f.productId).orElseThrow(() -> ApiException.bad("Chọn sản phẩm"));
        if (f.startTime == null || f.endTime == null || !f.endTime.isAfter(f.startTime)) throw ApiException.bad("Thời gian không hợp lệ");
        if (f.salePrice <= 0 || f.salePrice >= p.price) throw ApiException.bad("Giá flash sale phải nhỏ hơn giá bán");
        if (f.quota <= 0) throw ApiException.bad("Số lượng suất phải lớn hơn 0");
        f.productName = p.name; return f;
    }

    // ===== Banner =====
    @GetMapping("/banners") public List<Banner> banners() { return banners.findAllByOrderByPositionAscSortOrderAsc(); }
    @PostMapping("/banners") public Banner createBanner(@RequestBody Banner b) { b.id = null; return banners.save(b); }
    @PutMapping("/banners/{id}") public Banner updateBanner(@PathVariable Long id, @RequestBody Banner b) { b.id = id; return banners.save(b); }
    @DeleteMapping("/banners/{id}") public void deleteBanner(@PathVariable Long id) { banners.deleteById(id); }

    // ===== Báo cáo CSV (chỉ ADMIN) =====
    @GetMapping("/reports/{kind}.csv")
    public ResponseEntity<byte[]> report(@PathVariable String kind) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        switch (kind) {
            case "orders" -> {
                sb.append("Mã đơn,Ngày,Khách hàng,SĐT,Tỉnh/Thành,Trạng thái,Thanh toán,TT thanh toán,Tạm tính,Giảm giá,Điểm,Phí ship,Tổng,Đã thu\n");
                for (var o : orders.findAllByOrderByCreatedAtDesc()) sb.append(csv(o.code, o.createdAt, o.buyerName, o.phone, o.city, o.status, o.paymentMethod,
                        o.paymentStatus, o.subtotal, o.discount, o.pointsDiscount, o.shippingFee, o.total, o.paidAmount));
            }
            case "products" -> {
                sb.append("ID,SKU,Tên,Thương hiệu,Danh mục,Tỷ lệ,Tuổi,Giá,Giá cũ,Tồn,Đã bán,Đánh giá,Đang bán\n");
                for (var p : products.findAll(Sort.by("id"))) sb.append(csv(p.id, p.sku, p.name, p.brand == null ? "" : p.brand.name, p.type, p.scale, p.ageMin + "+",
                        p.price, p.oldPrice, p.stock, p.sold, p.rating, p.active));
            }
            case "customers" -> {
                sb.append("ID,Email,Họ tên,SĐT,Quyền,Hạng,Điểm,Số đơn,Tổng chi,Khoá,Ngày tạo\n");
                for (var m : customers()) { User u = (User) m.get("user");
                    sb.append(csv(u.id, u.email, u.fullName, u.phone, u.role, u.tier, u.points, m.get("orderCount"), m.get("totalSpent"), u.locked, u.createdAt)); }
            }
            case "inventory" -> {
                sb.append("Thời gian,Sản phẩm,Thay đổi,Tồn sau,Lý do,Người thực hiện\n");
                for (var s : stock.findTop200ByOrderByCreatedAtDesc()) sb.append(csv(s.createdAt, s.productName, s.delta, s.after, s.reason, s.byUser));
            }
            default -> throw ApiException.notFound("Không có báo cáo " + kind);
        }
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + kind + ".csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8)).body(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String csv(Object... v) {
        return Arrays.stream(v).map(x -> x == null ? "" : x.toString()).map(s -> s.contains(",") || s.contains("\"") || s.contains("\n") ? "\"" + s.replace("\"", "\"\"") + "\"" : s)
                .collect(Collectors.joining(",")) + "\n";
    }
}
