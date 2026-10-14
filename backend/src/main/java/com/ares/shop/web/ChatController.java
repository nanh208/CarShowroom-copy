package com.ares.shop.web;

import com.ares.shop.model.ChatMessage;
import com.ares.shop.repo.ChatRepo;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** Live chat đơn giản dùng polling. Khách dùng sessionId ngẫu nhiên lưu ở trình duyệt. */
@RestController
public class ChatController {
    private final ChatRepo chat;

    public ChatController(ChatRepo chat) { this.chat = chat; }

    public record Msg(String content, String customerName) {}

    @GetMapping("/api/chat/{session}")
    public List<ChatMessage> poll(@PathVariable String session, @RequestParam(defaultValue = "0") Long after) {
        return chat.findBySessionIdAndIdGreaterThanOrderByIdAsc(session, after);
    }

    @PostMapping("/api/chat/{session}")
    public ChatMessage send(@PathVariable String session, @RequestBody Msg m) {
        if (m.content() == null || m.content().isBlank()) throw ApiException.bad("Tin nhắn trống");
        boolean first = chat.findFirstBySessionIdOrderByIdDesc(session) == null;
        ChatMessage c = new ChatMessage();
        c.sessionId = session; c.content = m.content().trim(); c.fromAdmin = false;
        var u = Current.userOrNull();
        c.customerName = u != null ? u.fullName : (m.customerName() == null || m.customerName().isBlank() ? "Khách" : m.customerName());
        chat.save(c);
        if (first) {
            ChatMessage auto = new ChatMessage();
            auto.sessionId = session; auto.fromAdmin = true; auto.customerName = c.customerName;
            auto.content = "Xin chào " + c.customerName + "! Cảm ơn bạn đã liên hệ ARES Models. Nhân viên tư vấn sẽ phản hồi trong giây lát. Hotline: 1900 6868.";
            chat.save(auto);
        }
        return c;
    }

    // ===== Admin =====
    @GetMapping("/api/admin/chat/sessions")
    public List<Map<String, Object>> sessions() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (String s : chat.sessions()) {
            ChatMessage last = chat.findFirstBySessionIdOrderByIdDesc(s);
            ChatMessage firstCus = chat.findFirstBySessionIdAndFromAdminFalseOrderByIdAsc(s);
            out.add(Map.of("sessionId", s, "customerName", firstCus != null ? firstCus.customerName : "Khách",
                    "lastMessage", last.content, "lastAt", last.createdAt, "waiting", !last.fromAdmin));
        }
        return out;
    }

    @PostMapping("/api/admin/chat/{session}")
    public ChatMessage reply(@PathVariable String session, @RequestBody Msg m) {
        ChatMessage c = new ChatMessage();
        c.sessionId = session; c.fromAdmin = true; c.content = m.content(); c.customerName = Current.user().fullName;
        return chat.save(c);
    }
}
