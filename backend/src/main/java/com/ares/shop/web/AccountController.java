package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController @RequestMapping("/api/account")
public class AccountController {
    private final UserRepo users; private final AddressRepo addresses; private final OrderRepo orders;
    private final BookingRepo bookings; private final PasswordEncoder enc;

    public AccountController(UserRepo users, AddressRepo addresses, OrderRepo orders, BookingRepo bookings, PasswordEncoder enc) {
        this.users = users; this.addresses = addresses; this.orders = orders; this.bookings = bookings; this.enc = enc;
    }

    public record ProfileReq(String fullName, String phone, String avatarUrl) {}
    public record PasswordReq(String currentPassword, String newPassword) {}

    @PutMapping("/profile")
    public User profile(@RequestBody ProfileReq r) {
        User u = users.findById(Current.user().id).orElseThrow();
        if (r.fullName() != null) u.fullName = r.fullName();
        u.phone = r.phone(); u.avatarUrl = r.avatarUrl();
        return users.save(u);
    }

    @PutMapping("/password")
    public Map<String, String> password(@RequestBody PasswordReq r) {
        User u = users.findById(Current.user().id).orElseThrow();
        if (u.password != null && (r.currentPassword() == null || !enc.matches(r.currentPassword(), u.password)))
            throw ApiException.bad("Mật khẩu hiện tại không đúng");
        if (r.newPassword() == null || r.newPassword().length() < 6) throw ApiException.bad("Mật khẩu mới tối thiểu 6 ký tự");
        u.password = enc.encode(r.newPassword()); users.save(u);
        return Map.of("message", "Đã đổi mật khẩu");
    }

    // ===== Sổ địa chỉ =====
    @GetMapping("/addresses")
    public List<Address> list() { return addresses.findByUserIdOrderByIdDesc(Current.user().id); }

    @PostMapping("/addresses") @Transactional
    public Address create(@RequestBody Address a) {
        a.id = null; a.user = Current.user();
        if (list().isEmpty()) a.isDefault = true;
        if (a.isDefault) clearDefault();
        return addresses.save(a);
    }

    @PutMapping("/addresses/{id}") @Transactional
    public Address update(@PathVariable Long id, @RequestBody Address a) {
        Address cur = own(id);
        if (a.isDefault) clearDefault();
        a.id = cur.id; a.user = cur.user;
        return addresses.save(a);
    }

    @DeleteMapping("/addresses/{id}")
    public void delete(@PathVariable Long id) { addresses.delete(own(id)); }

    private Address own(Long id) {
        return addresses.findById(id).filter(x -> x.user.id.equals(Current.user().id))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy địa chỉ"));
    }

    private void clearDefault() {
        list().forEach(x -> { if (x.isDefault) { x.isDefault = false; addresses.save(x); } });
    }

    // ===== Lịch sử =====
    @GetMapping("/orders")
    public List<CustomerOrder> orders() { return orders.findByUserIdOrderByCreatedAtDesc(Current.user().id); }

    @GetMapping("/bookings")
    public List<Booking> bookings() { return bookings.findByUserIdOrderByCreatedAtDesc(Current.user().id); }
}
