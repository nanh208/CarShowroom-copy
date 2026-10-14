package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.ares.shop.service.OrderService;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController @RequestMapping("/api/admin")
public class AdminController {
    private final ProductRepo products; private final BrandRepo brands; private final OrderRepo orders; private final UserRepo users;
    private final PromotionRepo promos; private final BookingRepo bookings; private final OrderService orderService;

    public AdminController(ProductRepo products, BrandRepo brands, OrderRepo orders, UserRepo users, PromotionRepo promos,
                           BookingRepo bookings, OrderService orderService) {
        this.products = products; this.brands = brands; this.orders = orders; this.users = users;
        this.promos = promos; this.bookings = bookings; this.orderService = orderService;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        var all = orders.findAll();
        long revenue = all.stream().filter(o -> o.status != Enums.OrderStatus.CANCELLED).mapToLong(o -> o.paidAmount).sum();
        long pending = all.stream().filter(o -> o.status == Enums.OrderStatus.PENDING).count();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (var s : Enums.OrderStatus.values()) byStatus.put(s.name(), all.stream().filter(o -> o.status == s).count());
        return Map.of("revenue", revenue, "orders", all.size(), "pendingOrders", pending, "customers", users.count(),
                "products", products.count(), "lowStock", products.findAll().stream().filter(p -> p.stock <= 2).count(),
                "bookingsPending", bookings.findAll().stream().filter(b -> b.status == Enums.BookingStatus.PENDING).count(),
                "ordersByStatus", byStatus, "recentOrders", orders.findAllByOrderByCreatedAtDesc().stream().limit(5).toList());
    }

    // ===== Sản phẩm =====
    @GetMapping("/products")
    public Page<Product> products(@RequestParam(required = false) String q, @RequestParam(required = false) Long brandId,
                                  @RequestParam(required = false) String type, @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return products.findAll(CatalogController.filter(q, brandId, type, null, null, null, null, false),
                PageRequest.of(page, size, Sort.by("id").descending()));
    }

    @PostMapping("/products")
    public Product createProduct(@RequestBody Product p) { p.id = null; return products.save(fixBrand(p)); }

    @PutMapping("/products/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody Product p) {
        Product cur = products.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm"));
        p.id = id; p.createdAt = cur.createdAt;
        return products.save(fixBrand(p));
    }

    @DeleteMapping("/products/{id}")
    public void deleteProduct(@PathVariable Long id) {
        // xoá mềm để giữ lịch sử đơn hàng
        products.findById(id).ifPresent(p -> { p.active = false; products.save(p); });
    }

    private Product fixBrand(Product p) {
        if (p.name == null || p.name.isBlank()) throw ApiException.bad("Tên sản phẩm bắt buộc");
        if (p.brand != null && p.brand.id != null) p.brand = brands.findById(p.brand.id).orElse(null);
        return p;
    }

    // ===== Thương hiệu =====
    @PostMapping("/brands") public Brand createBrand(@RequestBody Brand b) { b.id = null; return brands.save(b); }
    @PutMapping("/brands/{id}") public Brand updateBrand(@PathVariable Long id, @RequestBody Brand b) { b.id = id; return brands.save(b); }
    @DeleteMapping("/brands/{id}") public void deleteBrand(@PathVariable Long id) {
        if (products.findAll(CatalogController.filter(null, id, null, null, null, null, null, false)).size() > 0)
            throw ApiException.bad("Thương hiệu đang có sản phẩm, không thể xoá");
        brands.deleteById(id);
    }

    // ===== Đơn hàng =====
    @GetMapping("/orders")
    public List<CustomerOrder> orders(@RequestParam(required = false) String status) {
        return orders.findAllByOrderByCreatedAtDesc().stream().filter(o -> status == null || status.isBlank() || o.status.name().equals(status)).toList();
    }

    public record StatusReq(String status) {}

    @PatchMapping("/orders/{id}/status")
    public CustomerOrder orderStatus(@PathVariable Long id, @RequestBody StatusReq r) {
        CustomerOrder o = orders.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn"));
        var s = Enums.OrderStatus.valueOf(r.status());
        if (s == Enums.OrderStatus.CANCELLED) return orderService.cancel(o);
        if (o.status == Enums.OrderStatus.CANCELLED) throw ApiException.bad("Đơn đã huỷ");
        o.status = s;
        if (s == Enums.OrderStatus.COMPLETED && o.paymentMethod == Enums.PaymentMethod.COD) {
            o.paidAmount = o.total; o.paymentStatus = Enums.PaymentStatus.PAID;
        }
        return orders.save(o);
    }

    // ===== Khách hàng =====
    @GetMapping("/customers")
    public List<Map<String, Object>> customers() {
        var all = orders.findAll();
        return users.findAll(Sort.by("id").descending()).stream().map(u -> {
            var mine = all.stream().filter(o -> o.user != null && o.user.id.equals(u.id) && o.status != Enums.OrderStatus.CANCELLED).toList();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("user", u); m.put("orderCount", mine.size()); m.put("totalSpent", mine.stream().mapToLong(o -> o.total).sum());
            return m;
        }).toList();
    }

    public record UserPatch(Boolean locked, String role) {}

    @PatchMapping("/customers/{id}")
    public User patchUser(@PathVariable Long id, @RequestBody UserPatch r) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy khách hàng"));
        if (u.id.equals(Current.user().id)) throw ApiException.bad("Không thể tự thay đổi tài khoản của mình");
        if (r.locked() != null) u.locked = r.locked();
        if (r.role() != null) u.role = Enums.Role.valueOf(r.role());
        return users.save(u);
    }

    // ===== Ưu đãi =====
    @GetMapping("/promotions") public List<Promotion> promos() { return promos.findAll(Sort.by("id").descending()); }
    @PostMapping("/promotions") public Promotion createPromo(@RequestBody Promotion p) { p.id = null; return promos.save(checkPromo(p)); }
    @PutMapping("/promotions/{id}") public Promotion updatePromo(@PathVariable Long id, @RequestBody Promotion p) { p.id = id; return promos.save(checkPromo(p)); }
    @DeleteMapping("/promotions/{id}") public void deletePromo(@PathVariable Long id) { promos.deleteById(id); }

    private Promotion checkPromo(Promotion p) {
        if (p.code == null || p.code.isBlank()) throw ApiException.bad("Mã ưu đãi bắt buộc");
        if (p.discountPercent < 0 || p.discountPercent > 100) throw ApiException.bad("% giảm phải từ 0-100");
        p.code = p.code.trim().toUpperCase();
        return p;
    }

    // ===== Lịch hẹn =====
    @GetMapping("/bookings")
    public List<Booking> bookings(@RequestParam(required = false) String type) {
        return bookings.findAllByOrderByCreatedAtDesc().stream().filter(b -> type == null || type.isBlank() || b.type.name().equals(type)).toList();
    }

    @PatchMapping("/bookings/{id}/status")
    public Booking bookingStatus(@PathVariable Long id, @RequestBody StatusReq r) {
        Booking b = bookings.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy lịch hẹn"));
        b.status = Enums.BookingStatus.valueOf(r.status());
        return bookings.save(b);
    }
}
