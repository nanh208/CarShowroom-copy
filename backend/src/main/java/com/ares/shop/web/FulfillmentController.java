package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.ares.shop.service.CarrierService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Xuất kho nhiều bước: Nhặt hàng (quét SKU / đánh dấu từng món) → Đóng gói (cân, hộp, số kiện) → Bàn giao vận chuyển (GHN/GHTK mô phỏng: báo giá, tạo vận đơn).
 * Bàn giao chuyển đơn sang SHIPPING qua đúng luồng trạng thái hiện có (thông báo + email + sự kiện).
 * Tra cứu vận đơn công khai + webhook hãng (có khoá bí mật) để báo giao thành công.
 */
@RestController
public class FulfillmentController {
    private final ShipmentRepo ships; private final OrderRepo orders; private final ProductRepo products; private final CarrierService carriers; private final AdminController admin;
    public FulfillmentController(ShipmentRepo ships, OrderRepo orders, ProductRepo products, CarrierService carriers, AdminController admin) {
        this.ships = ships; this.orders = orders; this.products = products; this.carriers = carriers; this.admin = admin;
    }
    static final String WEBHOOK_KEY = "ares-carrier-sandbox";

    private CustomerOrder order(Long id) { return orders.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn")); }
    private Shipment ship(Long orderId) { return ships.findByOrderId(orderId).orElseThrow(() -> ApiException.bad("Đơn chưa bắt đầu xuất kho")); }
    int weight(CustomerOrder o) { return o.items.stream().mapToInt(i -> CarrierService.weightOf(i.productId == null ? null : products.findById(i.productId).orElse(null)) * i.quantity).sum() + 150; }
    long cod(CustomerOrder o) { return o.paymentStatus == Enums.PaymentStatus.PAID ? 0 : Math.max(0, o.total - o.paidAmount); }

    private Map<String, Object> view(CustomerOrder o, Shipment s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderId", o.id); m.put("code", o.code); m.put("buyerName", o.buyerName); m.put("city", o.city); m.put("address", o.shippingAddress); m.put("phone", o.phone);
        m.put("status", o.status); m.put("paymentStatus", o.paymentStatus); m.put("paymentMethod", o.paymentMethod); m.put("total", o.total); m.put("createdAt", o.createdAt);
        m.put("shippingMethod", o.shippingMethod); m.put("carrierPref", o.carrier); m.put("giftWrap", Boolean.TRUE.equals(o.giftWrap)); m.put("estWeight", weight(o)); m.put("cod", cod(o));
        m.put("stage", s == null ? "NEW" : s.stage); m.put("shipment", s);
        m.put("items", o.items.stream().map(i -> { Map<String, Object> x = new LinkedHashMap<>(); x.put("id", i.id); x.put("name", i.productName); x.put("variant", i.variantName); x.put("sku", i.sku);
            x.put("image", i.image); x.put("qty", i.quantity); x.put("picked", s == null ? 0 : s.picked.getOrDefault(i.id, 0)); return x; }).toList());
        int need = o.items.stream().mapToInt(i -> i.quantity).sum(), got = s == null ? 0 : s.picked.values().stream().mapToInt(Integer::intValue).sum();
        m.put("pickProgress", need == 0 ? 0 : got * 100 / need);
        return m;
    }

    /** Hàng đợi: đơn đã xác nhận chờ xuất kho + đơn đã bàn giao trong 3 ngày gần nhất. */
    @GetMapping("/api/admin/fulfillment")
    public List<Map<String, Object>> queue() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (CustomerOrder o : orders.findAllByOrderByCreatedAtDesc()) {
            Shipment s = ships.findByOrderId(o.id).orElse(null);
            boolean waiting = o.status == Enums.OrderStatus.CONFIRMED;
            boolean recent = s != null && s.shippedAt != null && s.shippedAt.isAfter(LocalDateTime.now().minusDays(3));
            if (waiting || recent) out.add(view(o, s));
        }
        out.sort(Comparator.comparing((Map<String, Object> m) -> List.of("PICKING", "PICKED", "PACKED", "NEW", "SHIPPED", "DELIVERED").indexOf(String.valueOf(m.get("stage")))));
        return out;
    }
    @GetMapping("/api/admin/fulfillment/{orderId}")
    public Map<String, Object> one(@PathVariable Long orderId) { CustomerOrder o = order(orderId); return view(o, ships.findByOrderId(orderId).orElse(null)); }

    @PostMapping("/api/admin/fulfillment/{orderId}/start") @Transactional
    public Map<String, Object> start(@PathVariable Long orderId) {
        CustomerOrder o = order(orderId);
        if (o.status != Enums.OrderStatus.CONFIRMED) throw ApiException.bad("Chỉ xuất kho đơn đã xác nhận (hiện: " + o.status + ")");
        Shipment s = ships.findByOrderId(orderId).orElseGet(() -> { Shipment n = new Shipment(); n.orderId = o.id; n.orderCode = o.code; n.pickStartedAt = LocalDateTime.now(); n.pickedBy = Current.user().fullName; return ships.save(n); });
        return view(o, s);
    }

    public record PickReq(Long itemId, String sku, Integer qty) {}
    /** Nhặt hàng: theo itemId hoặc quét SKU; qty mặc định 1 (âm để bỏ nhặt). */
    @PostMapping("/api/admin/fulfillment/{orderId}/pick") @Transactional
    public Map<String, Object> pick(@PathVariable Long orderId, @RequestBody PickReq r) {
        CustomerOrder o = order(orderId); Shipment s = ship(orderId);
        if (!"PICKING".equals(s.stage) && !"PICKED".equals(s.stage)) throw ApiException.bad("Đơn đã đóng gói — không nhặt thêm");
        int q = r.qty() == null ? 1 : r.qty();
        OrderItem it = o.items.stream().filter(i -> r.itemId() != null ? i.id.equals(r.itemId()) : r.sku() != null && r.sku().trim().equalsIgnoreCase(i.sku))
            .filter(i -> q < 0 || s.picked.getOrDefault(i.id, 0) < i.quantity).findFirst()
            .orElseThrow(() -> ApiException.bad(r.sku() != null ? "SKU " + r.sku() + " không thuộc đơn hoặc đã nhặt đủ" : "Món không thuộc đơn hoặc đã nhặt đủ"));
        int nv = Math.max(0, Math.min(it.quantity, s.picked.getOrDefault(it.id, 0) + q));
        s.picked.put(it.id, nv);
        boolean all = o.items.stream().allMatch(i -> s.picked.getOrDefault(i.id, 0) >= i.quantity);
        s.stage = all ? "PICKED" : "PICKING"; if (all) s.pickedAt = LocalDateTime.now();
        ships.save(s); return view(o, s);
    }

    public record PackReq(Integer weightGrams, String boxSize, Integer packages) {}
    @PostMapping("/api/admin/fulfillment/{orderId}/pack") @Transactional
    public Map<String, Object> pack(@PathVariable Long orderId, @RequestBody PackReq r) {
        CustomerOrder o = order(orderId); Shipment s = ship(orderId);
        if (!"PICKED".equals(s.stage)) throw ApiException.bad("Cần nhặt đủ hàng trước khi đóng gói");
        if (r.weightGrams() == null || r.weightGrams() < 50 || r.weightGrams() > 30000) throw ApiException.bad("Khối lượng 50 g – 30 kg");
        if (r.boxSize() == null || !List.of("S", "M", "L", "XL").contains(r.boxSize())) throw ApiException.bad("Chọn cỡ hộp S/M/L/XL");
        int pk = r.packages() == null ? 1 : r.packages(); if (pk < 1 || pk > 10) throw ApiException.bad("Số kiện 1–10");
        s.weightGrams = r.weightGrams(); s.boxSize = r.boxSize(); s.packages = pk; s.packedAt = LocalDateTime.now(); s.packedBy = Current.user().fullName; s.stage = "PACKED";
        ships.save(s); return view(o, s);
    }

    @GetMapping("/api/admin/fulfillment/{orderId}/rates")
    public List<CarrierService.Quote> rates(@PathVariable Long orderId) {
        CustomerOrder o = order(orderId); Shipment s = ships.findByOrderId(orderId).orElse(null);
        return carriers.quoteAll(o.city, s != null && s.weightGrams != null ? s.weightGrams : weight(o), cod(o));
    }

    public record ShipReq(String carrier, String service) {}
    @PostMapping("/api/admin/fulfillment/{orderId}/ship") @Transactional
    public Map<String, Object> handOver(@PathVariable Long orderId, @RequestBody ShipReq r) {
        CustomerOrder o = order(orderId); Shipment s = ship(orderId);
        if (!"PACKED".equals(s.stage)) throw ApiException.bad("Cần đóng gói trước khi bàn giao");
        String c = r.carrier() == null ? "" : r.carrier().toUpperCase();
        if (!CarrierService.CARRIERS.contains(c)) throw ApiException.bad("Hãng vận chuyển không hợp lệ");
        var q = carriers.quote(c, r.service(), o.city, s.weightGrams, cod(o));
        s.carrier = c; s.service = q.service(); s.fee = q.fee(); s.codFee = q.codFee(); s.codAmount = cod(o); s.trackingNo = carriers.newTracking(c);
        s.shippedAt = LocalDateTime.now(); s.shippedBy = Current.user().fullName; s.stage = "SHIPPED";
        ships.save(s);
        admin.orderStatus(o.id, new AdminController.StatusReq("SHIPPING", "Bàn giao " + c + " · " + s.trackingNo));
        CustomerOrder fresh = order(orderId); fresh.carrier = c; fresh.trackingNo = s.trackingNo; orders.save(fresh);
        return view(fresh, s);
    }

    /** Tra cứu vận đơn công khai (mã vận đơn hoặc mã đơn). */
    @GetMapping("/api/public/carrier/track/{no}")
    public Map<String, Object> track(@PathVariable String no) {
        Shipment s = ships.findByTrackingNo(no.trim().toUpperCase()).or(() -> ships.findByOrderCode(no.trim().toUpperCase()))
            .orElseThrow(() -> ApiException.notFound("Không tìm thấy vận đơn"));
        if (s.trackingNo == null) throw ApiException.notFound("Đơn chưa bàn giao vận chuyển");
        CustomerOrder o = order(s.orderId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("trackingNo", s.trackingNo); m.put("carrier", s.carrier); m.put("carrierName", CarrierService.NAMES.get(s.carrier)); m.put("service", s.service);
        m.put("orderCode", o.code); m.put("delivered", s.deliveredAt != null); m.put("etaTo", o.etaTo);
        m.put("events", carriers.events(s.carrier, s.trackingNo, s.shippedAt, o.city, s.deliveredAt != null));
        return m;
    }

    public record Hook(String trackingNo, String status, String key) {}
    /** Webhook hãng vận chuyển (sandbox): DELIVERED → hoàn tất đơn. Yêu cầu khoá bí mật. */
    @PostMapping("/api/public/carrier/webhook") @Transactional
    public Map<String, Object> webhook(@RequestBody Hook h) {
        if (!WEBHOOK_KEY.equals(h.key())) throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "Sai khoá webhook");
        Shipment s = ships.findByTrackingNo(String.valueOf(h.trackingNo()).toUpperCase()).orElseThrow(() -> ApiException.notFound("Không tìm thấy vận đơn"));
        if ("DELIVERED".equals(h.status()) && s.deliveredAt == null) {
            s.deliveredAt = LocalDateTime.now(); s.stage = "DELIVERED"; ships.save(s);
            CustomerOrder o = order(s.orderId);
            if (o.status == Enums.OrderStatus.SHIPPING) {
                if (o.paymentMethod == Enums.PaymentMethod.COD && o.paymentStatus != Enums.PaymentStatus.PAID) admin.markPaid(o.id);
                admin.orderStatus(o.id, new AdminController.StatusReq("COMPLETED", s.carrier + " giao thành công"));
            }
        }
        return Map.of("ok", true, "stage", s.stage);
    }

    /** Admin: giả lập hãng báo giao thành công (gọi chính webhook). */
    @PostMapping("/api/admin/fulfillment/{orderId}/simulate-delivered")
    public Map<String, Object> simulate(@PathVariable Long orderId) {
        Shipment s = ship(orderId); if (s.trackingNo == null) throw ApiException.bad("Đơn chưa bàn giao");
        return webhook(new Hook(s.trackingNo, "DELIVERED", WEBHOOK_KEY));
    }
}
