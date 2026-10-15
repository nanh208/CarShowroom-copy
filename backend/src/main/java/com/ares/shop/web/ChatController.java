package com.ares.shop.web;

import com.ares.shop.model.ChatMessage;
import com.ares.shop.repo.ChatRepo;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** Live chat: REST để gửi/tải lịch sử, đẩy thời gian thực qua STOMP (/topic/chat/{session}, /topic/admin/chat). Polling vẫn hoạt động làm dự phòng. */
@RestController
public class ChatController {
    private final ChatRepo chat; private final org.springframework.messaging.simp.SimpMessagingTemplate ws;

    public ChatController(ChatRepo chat, org.springframework.messaging.simp.SimpMessagingTemplate ws) { this.chat = chat; this.ws = ws; }

    /** Lưu + phát tới khách (phiên) và hộp thư nhân viên. */
    private ChatMessage push(ChatMessage c) {
        chat.save(c);
        ws.convertAndSend("/topic/chat/" + c.sessionId, c);
        ws.convertAndSend("/topic/admin/chat", Map.of("sessionId", c.sessionId, "message", c));
        return c;
    }

    /** Chỉ báo "đang gõ…" (không lưu). who = customer|staff. */
    @org.springframework.messaging.handler.annotation.MessageMapping("/chat/{session}/typing")
    public void typing(@org.springframework.messaging.handler.annotation.DestinationVariable String session, Map<String, Object> body) {
        if (session.length() > 64) return;
        String who = "staff".equals(String.valueOf(body.get("who"))) ? "staff" : "customer";
        ws.convertAndSend("/topic/chat/" + session + "/typing", Map.of("who", who, "on", Boolean.TRUE.equals(body.get("on"))));
        if ("customer".equals(who)) ws.convertAndSend("/topic/admin/chat/typing", Map.of("sessionId", session, "on", Boolean.TRUE.equals(body.get("on"))));
    }

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
        if (c.content.length() > 2000) throw ApiException.bad("Tin nhắn quá dài");
        push(c);
        if (first) {
            ChatMessage auto = new ChatMessage();
            auto.sessionId = session; auto.fromAdmin = true; auto.customerName = c.customerName;
            auto.content = "Xin chào " + c.customerName + "! Cảm ơn bạn đã liên hệ ARES Auto. Nhân viên tư vấn sẽ phản hồi trong giây lát. Hotline: 1900 6868.";
            push(auto);
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
        if (m.content() == null || m.content().isBlank()) throw ApiException.bad("Tin nhắn trống");
        c.sessionId = session; c.fromAdmin = true; c.content = m.content().trim(); c.customerName = Current.user().fullName;
        return push(c);
    }
}
