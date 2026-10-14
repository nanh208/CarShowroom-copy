package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController @RequestMapping("/api/bookings")
public class BookingController {
    private final BookingRepo bookings; private final ProductRepo products;

    public BookingController(BookingRepo bookings, ProductRepo products) { this.bookings = bookings; this.products = products; }

    /** Đăng ký lái thử (TEST_DRIVE) hoặc đặt lịch sửa chữa & bảo dưỡng (MAINTENANCE). */
    @PostMapping
    public Booking create(@RequestBody Booking b) {
        if (b.type == null) throw ApiException.bad("Thiếu loại lịch hẹn");
        if (b.name == null || b.name.isBlank() || b.phone == null || b.phone.isBlank()) throw ApiException.bad("Vui lòng nhập họ tên và số điện thoại");
        if (b.date == null || b.date.isBefore(LocalDate.now())) throw ApiException.bad("Ngày hẹn không hợp lệ");
        b.id = null; b.status = Enums.BookingStatus.PENDING; b.user = Current.userOrNull();
        if (b.productId != null) products.findById(b.productId).ifPresent(p -> b.productName = p.name);
        return bookings.save(b);
    }

    @PostMapping("/{id}/cancel")
    public Booking cancel(@PathVariable Long id) {
        User u = Current.user();
        Booking b = bookings.findById(id).filter(x -> x.user != null && x.user.id.equals(u.id))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy lịch hẹn"));
        b.status = Enums.BookingStatus.CANCELLED;
        return bookings.save(b);
    }
}
