package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.ares.shop.service.*;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

/** v16 vận hành: dự báo tồn kho, hàng chờ duyệt gian lận, nhãn GHN/GHTK PDF. */
@RestController
@RequestMapping("/api/admin")
public class V16OpsController {
    final ForecastService forecast; final FraudService fraud; final LabelPdfService pdf; final OrderRepo orders; final OrderService orderService;
    final ShipmentRepo shipments; final ProductRepo products; final CarrierService carriers;
    public V16OpsController(ForecastService forecast, FraudService fraud, LabelPdfService pdf, OrderRepo orders, OrderService orderService, ShipmentRepo shipments, ProductRepo products, CarrierService carriers) {
        this.forecast = forecast; this.fraud = fraud; this.pdf = pdf; this.orders = orders; this.orderService = orderService; this.shipments = shipments; this.products = products; this.carriers = carriers;
    }
    @org.springframework.beans.factory.annotation.Value("${app.shop.name:ARES Toys}") String shopName;
    @org.springframework.beans.factory.annotation.Value("${app.shop.phone:1900 6868}") String shopPhone;
    @org.springframework.beans.factory.annotation.Value("${app.shop.address:123 Nguyễn Văn Linh, Tân Phong, Quận 7, TP. Hồ Chí Minh}") String shopAddress;

    // ---------- dự báo ----------
    @GetMapping("/forecast")
    public Map<String, Object> forecast(@RequestParam(defaultValue = "90") int days, @RequestParam(defaultValue = "30") int horizon,
                                        @RequestParam(defaultValue = "7") int lead, @RequestParam(defaultValue = "0.95") double serviceLevel) {
        return forecast.run(days, horizon, lead, serviceLevel);
    }

    // ---------- gian lận ----------
    Map<String, Object> view(CustomerOrder o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.id); m.put("code", o.code); m.put("buyerName", o.buyerName); m.put("phone", o.phone); m.put("email", o.email); m.put("total", o.total);
        m.put("paymentMethod", o.paymentMethod); m.put("status", o.status); m.put("createdAt", o.createdAt); m.put("guest", o.user == null); m.put("clientIp", o.clientIp);
        m.put("fraudScore", o.fraudScore); m.put("fraudStatus", o.fraudStatus); m.put("fraudReviewedBy", o.fraudReviewedBy); m.put("fraudReviewedAt", o.fraudReviewedAt);
        List<Map<String, Object>> rs = new ArrayList<>();
        if (o.fraudReasons != null && !o.fraudReasons.isBlank()) for (String r : o.fraudReasons.split("; ")) { String[] p = r.split(":", 3); if (p.length == 3) rs.add(Map.of("code", p[0], "points", Integer.parseInt(p[1]), "reason", p[2])); }
        m.put("signals", rs); m.put("items", o.items.size());
        return m;
    }
    @GetMapping("/fraud/queue")
    public Map<String, Object> queue(@RequestParam(defaultValue = "REVIEW") String status) {
        Set<String> want = "ALL".equals(status) ? Set.of("REVIEW", "WATCH", "APPROVED", "REJECTED") : Set.of(status.split(","));
        var list = orders.findAllByOrderByCreatedAtDesc().stream().filter(o -> o.fraudStatus != null && want.contains(o.fraudStatus))
            .sorted(Comparator.comparing((CustomerOrder o) -> o.fraudScore == null ? 0 : -o.fraudScore)).limit(200).map(this::view).toList();
        Map<String, Long> counts = new TreeMap<>(); orders.findAll().forEach(o -> { if (o.fraudStatus != null) counts.merge(o.fraudStatus, 1L, Long::sum); });
        return Map.of("items", list, "counts", counts, "reviewAt", FraudService.REVIEW_AT, "watchAt", FraudService.WATCH_AT);
    }
    @GetMapping("/fraud/orders/{id}") public Map<String, Object> one(@PathVariable Long id) { return view(get(id)); }
    CustomerOrder get(Long id) { return orders.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn")); }
    public record Decision(String note) {}
    @PostMapping("/fraud/orders/{id}/approve") @Transactional
    public Map<String, Object> approve(@PathVariable Long id, @RequestBody(required = false) Decision d) {
        CustomerOrder o = get(id); if (!"REVIEW".equals(o.fraudStatus) && !"WATCH".equals(o.fraudStatus)) throw ApiException.bad("Đơn không ở trạng thái chờ duyệt");
        o.fraudStatus = "APPROVED"; o.fraudReviewedBy = Current.user().email; o.fraudReviewedAt = LocalDateTime.now(); o.timeline.put("FRAUD_APPROVED", LocalDateTime.now());
        return view(orders.save(o));
    }
    @PostMapping("/fraud/orders/{id}/reject") @Transactional
    public Map<String, Object> reject(@PathVariable Long id, @RequestBody(required = false) Decision d) {
        CustomerOrder o = get(id); if (!"REVIEW".equals(o.fraudStatus) && !"WATCH".equals(o.fraudStatus)) throw ApiException.bad("Đơn không ở trạng thái chờ duyệt");
        if (o.status != Enums.OrderStatus.PENDING && o.status != Enums.OrderStatus.CONFIRMED) throw ApiException.bad("Chỉ huỷ được đơn chưa giao");
        o.fraudStatus = "REJECTED"; o.fraudReviewedBy = Current.user().email; o.fraudReviewedAt = LocalDateTime.now();
        orderService.cancel(o, "Huỷ do nghi ngờ gian lận" + (d != null && d.note() != null && !d.note().isBlank() ? ": " + d.note() : ""));
        return view(get(id));
    }
    @PostMapping("/fraud/rescan") @Transactional
    public Map<String, Object> rescan() {
        int n = 0, review = 0;
        for (CustomerOrder o : orders.findAll()) if (o.status == Enums.OrderStatus.PENDING || o.status == Enums.OrderStatus.CONFIRMED) { fraud.assess(o); n++; if ("REVIEW".equals(o.fraudStatus)) review++; }
        return Map.of("scanned", n, "review", review);
    }

    // ---------- nhãn PDF ----------
    @GetMapping("/labels/pdf")
    @Transactional
    public ResponseEntity<byte[]> labels(@RequestParam String ids, @RequestParam(required = false) String carrier) throws Exception {
        List<Long> list = new ArrayList<>();
        for (String s : ids.split(",")) { try { list.add(Long.parseLong(s.trim())); } catch (NumberFormatException ignored) {} }
        if (list.isEmpty() || list.size() > 200) throw ApiException.bad("Chọn 1–200 đơn để in nhãn");
        if (carrier != null && !carrier.isBlank() && !Set.of("GHN", "GHTK").contains(carrier.toUpperCase())) throw ApiException.bad("Hãng vận chuyển: GHN hoặc GHTK");
        List<LabelPdfService.Label> labels = new ArrayList<>();
        for (Long id : list) {
            CustomerOrder o = orders.findById(id).orElse(null); if (o == null) continue;
            if (o.status == Enums.OrderStatus.CANCELLED) throw ApiException.bad("Đơn " + o.code + " đã huỷ — không in nhãn");
            if ("REVIEW".equals(o.fraudStatus)) throw ApiException.bad("Đơn " + o.code + " đang chờ duyệt gian lận — không in nhãn");
            Shipment sh = shipments.findByOrderId(o.id).orElse(null);
            String c = carrier != null && !carrier.isBlank() ? carrier.toUpperCase() : sh != null && sh.carrier != null ? sh.carrier : o.carrier != null ? o.carrier : "GHN";
            String tn = sh != null && sh.trackingNo != null && c.equals(sh.carrier) ? sh.trackingNo : o.trackingNo != null && c.equals(o.carrier) ? o.trackingNo : null;
            if (tn == null) { tn = carriers.newTracking(c); o.carrier = c; o.trackingNo = tn; orders.save(o); }
            int grams = sh != null && sh.weightGrams != null ? sh.weightGrams : o.items.stream().mapToInt(i -> products.findById(i.productId == null ? -1L : i.productId).map(CarrierService::weightOf).orElse(500) * i.quantity).sum();
            long cod = o.paymentStatus == Enums.PaymentStatus.PAID ? 0 : Math.max(0, o.total - o.paidAmount);
            String svc = sh != null && sh.service != null ? sh.service : "EXPRESS".equalsIgnoreCase(o.shippingMethod) ? "EXPRESS" : "STANDARD";
            labels.add(new LabelPdfService.Label(o, c, tn, svc, Math.max(100, grams), cod, sortCode(o.city, c)));
        }
        byte[] b = pdf.render(labels, shopName, shopPhone, shopAddress);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"labels-" + labels.size() + ".pdf\"")
            .header(HttpHeaders.CACHE_CONTROL, "no-store").body(b);
    }
    static String sortCode(String city, String carrier) {
        String n = city == null ? "" : BotController.norm(city);
        String r = n.contains("ho chi minh") || n.contains("hcm") ? "HCM" : n.contains("ha noi") ? "HN" : n.contains("da nang") ? "DN" : n.contains("can tho") ? "CT" : n.contains("hai phong") ? "HP" : "TINH";
        return ("GHTK".equals(carrier) ? "K-" : "N-") + r;
    }
}
