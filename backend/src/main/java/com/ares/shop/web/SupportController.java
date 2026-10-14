package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.ares.shop.service.NotifyService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/** Phiếu hỗ trợ (ticket) giữa khách hàng và nhân viên. */
@RestController
public class SupportController {
    private final TicketRepo tickets; private final NotifyService notify;

    public SupportController(TicketRepo tickets, NotifyService notify) { this.tickets = tickets; this.notify = notify; }

    public record NewTicket(String subject, String category, String orderCode, String content, List<String> attachments) {}
    public record Reply(String content, String status, List<String> attachments) {}

    @GetMapping("/api/tickets")
    public List<Ticket> mine() { return tickets.findByUserIdOrderByUpdatedAtDesc(Current.user().id); }

    @PostMapping("/api/tickets") @Transactional
    public Ticket create(@RequestBody NewTicket r) {
        User u = Current.user();
        if (r.subject() == null || r.subject().isBlank() || r.content() == null || r.content().trim().length() < 10)
            throw ApiException.bad("Vui lòng nhập tiêu đề và nội dung (tối thiểu 10 ký tự)");
        Ticket t = new Ticket(); t.user = u; t.subject = r.subject().trim(); t.category = r.category() == null ? "Khác" : r.category(); t.orderCode = r.orderCode();
        t.code = "HT" + String.valueOf(System.currentTimeMillis()).substring(5);
        add(t, false, u.fullName, r.content().trim(), r.attachments());
        Ticket saved = tickets.save(t);
        notify.send(u, "SYSTEM", "Đã tạo phiếu hỗ trợ " + saved.code, "Chúng tôi sẽ phản hồi trong vòng 24 giờ.", "/support/tickets/" + saved.code);
        return saved;
    }

    @GetMapping("/api/tickets/{code}")
    public Ticket get(@PathVariable String code) { return own(code); }

    @PostMapping("/api/tickets/{code}/reply") @Transactional
    public Ticket reply(@PathVariable String code, @RequestBody Reply r) {
        Ticket t = own(code);
        if ("CLOSED".equals(r.status())) { t.status = Enums.TicketStatus.CLOSED; t.updatedAt = LocalDateTime.now(); return tickets.save(t); }
        if ((r.content() == null || r.content().isBlank()) && (r.attachments() == null || r.attachments().isEmpty())) throw ApiException.bad("Nội dung trống");
        add(t, false, Current.user().fullName, r.content() == null ? "" : r.content().trim(), r.attachments()); t.status = Enums.TicketStatus.OPEN;
        return tickets.save(t);
    }

    // ===== Admin / Staff =====
    @GetMapping("/api/admin/tickets")
    public List<Ticket> all(@RequestParam(required = false) String status) {
        return tickets.findAllByOrderByUpdatedAtDesc().stream().filter(t -> status == null || status.isBlank() || t.status.name().equals(status)).toList();
    }

    @PostMapping("/api/admin/tickets/{id}/reply") @Transactional
    public Ticket staffReply(@PathVariable Long id, @RequestBody Reply r) {
        Ticket t = tickets.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy phiếu"));
        if (r.content() != null && !r.content().isBlank()) {
            add(t, true, "CSKH " + Current.user().fullName, r.content().trim(), r.attachments()); t.status = Enums.TicketStatus.ANSWERED;
            notify.send(t.user, "SYSTEM", "Phiếu " + t.code + " có phản hồi mới", r.content().trim(), "/support/tickets/" + t.code);
        }
        if (r.status() != null && !r.status().isBlank()) t.status = Enums.TicketStatus.valueOf(r.status());
        return tickets.save(t);
    }

    private void add(Ticket t, boolean staff, String author, String content, List<String> files) {
        TicketMessage m = new TicketMessage(); m.ticket = t; m.fromStaff = staff; m.author = author; m.content = content;
        if (files != null) files.stream().filter(f -> f != null && f.startsWith("/uploads/")).limit(5).forEach(m.attachments::add);
        t.messages.add(m); t.updatedAt = LocalDateTime.now();
    }

    private Ticket own(String code) {
        User u = Current.user();
        return tickets.findByCode(code).filter(t -> u.role != Enums.Role.USER || (t.user != null && t.user.id.equals(u.id)))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy phiếu hỗ trợ"));
    }
}
