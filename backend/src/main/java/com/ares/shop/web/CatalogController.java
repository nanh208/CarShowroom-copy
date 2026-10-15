package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.ares.shop.service.OrderService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController @RequestMapping("/api")
public class CatalogController {
    private final ProductRepo products; private final BrandRepo brands; private final PromotionRepo promos; private final OrderService orderService;
    private final CategoryRepo categories; private final VariantRepo variants; private final FlashSaleRepo flash; private final BannerRepo banners; private final OrderRepo orders;

    public CatalogController(ProductRepo products, BrandRepo brands, PromotionRepo promos, OrderService orderService, CategoryRepo categories,
                             VariantRepo variants, FlashSaleRepo flash, BannerRepo banners, OrderRepo orders) {
        this.products = products; this.brands = brands; this.promos = promos; this.orderService = orderService; this.categories = categories;
        this.variants = variants; this.flash = flash; this.banners = banners; this.orders = orders;
    }

    public record Filter(String q, Long brandId, String type, String grp, Long minPrice, Long maxPrice, String scale, Integer age,
                         Double minRating, Boolean inStock) {}

    public static Specification<Product> filter(Filter f, boolean onlyActive) {
        return (root, cq, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (onlyActive) p.add(cb.isTrue(root.get("active")));
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.join("brand", jakarta.persistence.criteria.JoinType.LEFT).get("name")), like),
                        cb.like(cb.lower(root.get("sku")), like)));
            }
            if (f.brandId() != null) p.add(cb.equal(root.get("brand").get("id"), f.brandId()));
            if (f.type() != null && !f.type().isBlank()) p.add(root.get("type").in(Arrays.asList(f.type().split(","))));
            if (f.grp() != null && !f.grp().isBlank()) p.add(cb.equal(root.get("grp"), f.grp()));
            if (f.scale() != null && !f.scale().isBlank()) p.add(root.get("scale").in(Arrays.asList(f.scale().split(","))));
            if (f.age() != null) p.add(cb.le(root.get("ageMin"), f.age()));
            if (f.minRating() != null) p.add(cb.ge(root.get("rating"), f.minRating()));
            if (f.minPrice() != null) p.add(cb.ge(root.get("price"), f.minPrice()));
            if (f.maxPrice() != null) p.add(cb.le(root.get("price"), f.maxPrice()));
            if (Boolean.TRUE.equals(f.inStock())) p.add(cb.gt(root.get("stock"), 0));
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    public static Sort sort(String s) {
        return switch (s == null ? "" : s) {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "bestseller" -> Sort.by("sold").descending();
            case "rating" -> Sort.by("rating").descending().and(Sort.by("reviewCount").descending());
            case "name" -> Sort.by("name");
            case "newest" -> Sort.by("createdAt").descending().and(Sort.by("id").descending());
            default -> Sort.by("featured").descending().and(Sort.by("isNew").descending()).and(Sort.by("sold").descending());
        };
    }

    @GetMapping("/products")
    public Page<Product> list(@RequestParam(required = false) String q, @RequestParam(required = false) Long brandId,
                              @RequestParam(required = false) String type, @RequestParam(required = false) String grp,
                              @RequestParam(required = false) Long minPrice, @RequestParam(required = false) Long maxPrice,
                              @RequestParam(required = false) String scale, @RequestParam(required = false) Integer age,
                              @RequestParam(required = false) Double minRating,
                              @RequestParam(required = false) Boolean inStock, @RequestParam(required = false) String sort,
                              @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
        return products.findAll(filter(new Filter(q, brandId, type, grp, minPrice, maxPrice, scale, age, minRating, inStock), true),
                PageRequest.of(page, Math.min(size, 60), sort(sort)));
    }

    /** Gợi ý tìm kiếm (autocomplete). */
    @GetMapping("/products/suggest")
    public List<Map<String, Object>> suggest(@RequestParam String q) {
        if (q == null || q.isBlank()) return List.of();
        return products.findAll(filter(new Filter(q, null, null, null, null, null, null, null, null, null), true), PageRequest.of(0, 6, sort("bestseller")))
                .map(p -> Map.<String, Object>of("id", p.id, "name", p.name, "price", p.price, "image", p.images.isEmpty() ? "" : p.images.get(0),
                        "brand", p.brand == null ? "" : p.brand.name)).getContent();
    }

    @GetMapping("/products/home")
    public Map<String, Object> home() {
        return Map.of("bestsellers", products.findTop8ByActiveTrueOrderBySoldDesc(),
                "newArrivals", products.findTop8ByActiveTrueAndIsNewTrueOrderByCreatedAtDesc(),
                "brands", brands.findAll(Sort.by("name")),
                "promotions", promos.findByActiveTrue().stream().filter(Promotion::isValidNow).filter(x -> x.isPublic).toList());
    }

    @GetMapping("/products/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        Product p = products.findById(id).filter(x -> x.active).orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm"));
        List<Product> related = p.brand == null ? List.of() : products.findTop4ByBrandIdAndIdNotAndActiveTrue(p.brand.id, p.id);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("product", p); m.put("related", related);
        m.put("variants", variants.findByProductIdOrderByIdAsc(id).stream().filter(v -> v.active).toList());
        m.put("flash", flash.findByProductId(id).stream().filter(FlashSale::isLive).toList());
        return m;
    }

    @GetMapping("/brands") public List<Brand> brands() { return brands.findAll(Sort.by("name")); }
    @GetMapping("/categories") public List<Category> categories() { return categories.findAllByOrderBySortOrderAsc(); }

    @GetMapping("/options")
    public Map<String, Object> options() {
        var scales = products.findAll().stream().map(p -> p.scale).filter(Objects::nonNull).distinct().sorted(Comparator.comparingInt(CatalogController::scaleN)).toList();
        return Map.of("scales", scales, "ages", List.of(3, 6, 8, 14), "freeShipFrom", OrderService.FREE_SHIP_FROM,
                "pointValue", OrderService.POINT_VALUE, "earnPer", OrderService.EARN_PER, "installmentMin", OrderService.INSTALLMENT_MIN,
                "cities", cities());
    }

    static int scaleN(String s) { try { return -Integer.parseInt(s.substring(s.indexOf(':') + 1)); } catch (Exception e) { return 0; } }

    public static List<String> cities() {
        List<String> c = new ArrayList<>(OrderService.INNER); c.addAll(OrderService.NEAR);
        c.addAll(List.of("Khánh Hoà", "Lâm Đồng", "Thừa Thiên Huế", "Nghệ An", "Thanh Hoá", "Quảng Ninh", "An Giang", "Kiên Giang", "Tỉnh khác"));
        return c;
    }

    @GetMapping("/shipping/quote")
    public Map<String, Object> ship(@RequestParam(required = false) String city, @RequestParam long amount) {
        long fee = OrderService.shippingFee(city, amount);
        return Map.of("fee", fee, "freeShipFrom", OrderService.FREE_SHIP_FROM, "remaining", Math.max(0, OrderService.FREE_SHIP_FROM - amount),
                "eta", city != null && OrderService.INNER.contains(city) ? "1–2 ngày" : "2–4 ngày");
    }

    @GetMapping("/promotions")
    public List<Promotion> promotions() { return promos.findByActiveTrue().stream().filter(Promotion::isValidNow).filter(x -> x.isPublic).toList(); }

    public record PromoCheck(String code, long subtotal) {}

    @PostMapping("/promotions/check")
    public Map<String, Object> check(@RequestBody PromoCheck r) {
        Promotion p = orderService.validPromo(r.code(), r.subtotal());
        return Map.of("promotion", p, "discount", orderService.discount(p, r.subtotal()), "freeShip", p.kind == Enums.VoucherKind.FREESHIP);
    }

    /** Flash sale đang chạy / sắp diễn ra, kèm thông tin sản phẩm. */
    @GetMapping("/flash-sales")
    public List<Map<String, Object>> flashSales() {
        var now = LocalDateTime.now();
        return flash.findAllByOrderByStartTimeDesc().stream().filter(f -> f.active && f.endTime.isAfter(now) && f.startTime.isBefore(now.plusDays(1)))
                .map(f -> products.findById(f.productId).filter(x -> x.active).map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("flash", f); m.put("product", p); m.put("live", f.isLive()); return m;
                }).orElse(null)).filter(Objects::nonNull).toList();
    }

    @GetMapping("/banners")
    public List<Banner> banners() { var n = LocalDateTime.now();
        return banners.findByActiveTrueOrderBySortOrderAsc().stream().filter(b -> (b.startAt == null || !n.isBefore(b.startAt)) && (b.endAt == null || n.isBefore(b.endAt))).toList(); }

    /** Tra cứu bảo hành theo mã đơn + số điện thoại. */
    @GetMapping("/warranty")
    public Map<String, Object> warranty(@RequestParam String code, @RequestParam String phone) {
        CustomerOrder o = orders.findByCode(code.trim().toUpperCase()).filter(x -> x.phone != null && x.phone.replaceAll("\\D", "").endsWith(phone.replaceAll("\\D", "").replaceFirst("^0", "")))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng khớp mã và số điện thoại"));
        LocalDateTime from = o.timeline.getOrDefault("COMPLETED", null);
        List<Map<String, Object>> items = o.items.stream().map(it -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", it.productName + (it.variantName == null ? "" : " – " + it.variantName)); m.put("months", it.warrantyMonths); m.put("image", it.image);
            if (from != null && it.warrantyMonths > 0) {
                var until = from.plusMonths(it.warrantyMonths);
                m.put("until", until); m.put("valid", until.isAfter(LocalDateTime.now()));
            } else m.put("valid", null);
            return m;
        }).toList();
        return Map.of("code", o.code, "status", o.status, "deliveredAt", from == null ? "" : from.toString(), "items", items);
    }
}
