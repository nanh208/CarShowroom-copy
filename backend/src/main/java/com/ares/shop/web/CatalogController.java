package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.ares.shop.service.CustomizerOptions;
import com.ares.shop.service.OrderService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController @RequestMapping("/api")
public class CatalogController {
    private final ProductRepo products; private final BrandRepo brands; private final PromotionRepo promos; private final OrderService orderService;

    public CatalogController(ProductRepo products, BrandRepo brands, PromotionRepo promos, OrderService orderService) {
        this.products = products; this.brands = brands; this.promos = promos; this.orderService = orderService;
    }

    public static Specification<Product> filter(String q, Long brandId, String type, String scale, Long minPrice, Long maxPrice,
                                                Boolean inStock, boolean onlyActive) {
        return (root, cq, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (onlyActive) p.add(cb.isTrue(root.get("active")));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.join("brand").get("name")), like)));
            }
            if (brandId != null) p.add(cb.equal(root.get("brand").get("id"), brandId));
            if (type != null && !type.isBlank()) p.add(cb.equal(root.get("type"), Enums.ProductType.valueOf(type)));
            if (scale != null && !scale.isBlank()) p.add(cb.equal(root.get("scale"), scale));
            if (minPrice != null) p.add(cb.ge(root.get("price"), minPrice));
            if (maxPrice != null) p.add(cb.le(root.get("price"), maxPrice));
            if (Boolean.TRUE.equals(inStock)) p.add(cb.gt(root.get("stock"), 0));
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    public static Sort sort(String s) {
        return switch (s == null ? "" : s) {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "bestseller" -> Sort.by("sold").descending();
            case "name" -> Sort.by("name");
            case "newest" -> Sort.by("createdAt").descending().and(Sort.by("id").descending());
            default -> Sort.by("featured").descending().and(Sort.by("isNew").descending()).and(Sort.by("sold").descending());
        };
    }

    @GetMapping("/products")
    public Page<Product> list(@RequestParam(required = false) String q, @RequestParam(required = false) Long brandId,
                              @RequestParam(required = false) String type, @RequestParam(required = false) String scale,
                              @RequestParam(required = false) Long minPrice, @RequestParam(required = false) Long maxPrice,
                              @RequestParam(required = false) Boolean inStock, @RequestParam(required = false) String sort,
                              @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
        return products.findAll(filter(q, brandId, type, scale, minPrice, maxPrice, inStock, true),
                PageRequest.of(page, Math.min(size, 60), sort(sort)));
    }

    @GetMapping("/products/home")
    public Map<String, Object> home() {
        return Map.of("bestsellers", products.findTop8ByActiveTrueOrderBySoldDesc(),
                "newArrivals", products.findTop8ByActiveTrueAndIsNewTrueOrderByCreatedAtDesc(),
                "brands", brands.findAll(Sort.by("name")),
                "promotions", promos.findByActiveTrue().stream().filter(Promotion::isValidNow).toList());
    }

    @GetMapping("/products/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        Product p = products.findById(id).filter(x -> x.active).orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm"));
        List<Product> related = p.brand == null ? List.of() : products.findTop4ByBrandIdAndIdNotAndActiveTrue(p.brand.id, p.id);
        return Map.of("product", p, "related", related);
    }

    @GetMapping("/brands")
    public List<Brand> brands() { return brands.findAll(Sort.by("name")); }

    @GetMapping("/options")
    public Map<String, Object> options() {
        return Map.of("customizer", CustomizerOptions.all(), "types", Enums.ProductType.values(),
                "scales", List.of("1:12", "1:18", "1:24", "1:32", "1:43", "1:64"));
    }

    @GetMapping("/promotions")
    public List<Promotion> promotions() { return promos.findByActiveTrue().stream().filter(Promotion::isValidNow).toList(); }

    public record PromoCheck(String code, long subtotal) {}

    @PostMapping("/promotions/check")
    public Map<String, Object> check(@RequestBody PromoCheck r) {
        Promotion p = orderService.validPromo(r.code(), r.subtotal());
        return Map.of("promotion", p, "discount", orderService.discount(p, r.subtotal()));
    }
}
