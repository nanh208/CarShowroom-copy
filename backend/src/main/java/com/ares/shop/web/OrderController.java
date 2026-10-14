package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.OrderRepo;
import com.ares.shop.service.OrderService;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service; private final OrderRepo orders;

    public OrderController(OrderService service, OrderRepo orders) { this.service = service; this.orders = orders; }

    @PostMapping
    public CustomerOrder place(@RequestBody OrderService.CheckoutReq r) { return service.place(Current.user(), r); }

    @GetMapping("/{code}")
    public CustomerOrder get(@PathVariable String code) { return own(code); }

    @PostMapping("/{code}/pay")
    public CustomerOrder pay(@PathVariable String code) { return service.payRemaining(own(code)); }

    @PostMapping("/{code}/cancel")
    public CustomerOrder cancel(@PathVariable String code) { return service.cancel(own(code)); }

    private CustomerOrder own(String code) {
        User u = Current.user();
        return orders.findByCode(code).filter(o -> u.role == Enums.Role.ADMIN || (o.user != null && o.user.id.equals(u.id)))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng"));
    }
}
