package com.ares.shop.config;

import com.ares.shop.security.JwtService;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.config.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.web.socket.config.annotation.*;

/**
 * STOMP qua WebSocket (/ws) cho live chat thời gian thực.
 * Topic: /topic/chat/{session} (tin nhắn + đang gõ của 1 phiên), /topic/admin/chat (hộp thư nhân viên).
 * Kênh /topic/admin/** chỉ cho ADMIN/STAFF: CONNECT phải kèm header Authorization: Bearer <jwt>.
 */
@Configuration @EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtService jwt;
    public WebSocketConfig(JwtService jwt) { this.jwt = jwt; }

    @Override public void registerStompEndpoints(StompEndpointRegistry r) { r.addEndpoint("/ws").setAllowedOriginPatterns("*"); }
    @Override public void configureMessageBroker(MessageBrokerRegistry r) { r.enableSimpleBroker("/topic"); r.setApplicationDestinationPrefixes("/app"); }

    @Override
    public void configureClientInboundChannel(ChannelRegistration reg) {
        reg.interceptors(new ChannelInterceptor() {
            @Override public Message<?> preSend(Message<?> msg, MessageChannel ch) {
                StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(msg, StompHeaderAccessor.class);
                if (acc == null) return msg;
                if (StompCommand.CONNECT.equals(acc.getCommand())) {
                    String h = acc.getFirstNativeHeader("Authorization"); String role = "GUEST";
                    if (h != null && h.startsWith("Bearer ")) try { role = String.valueOf(jwt.parse(h.substring(7)).get("role")); } catch (Exception ignored) { }
                    if (acc.getSessionAttributes() != null) acc.getSessionAttributes().put("role", role);
                }
                if (StompCommand.SUBSCRIBE.equals(acc.getCommand()) && acc.getDestination() != null && acc.getDestination().startsWith("/topic/admin")) {
                    Object role = acc.getSessionAttributes() == null ? null : acc.getSessionAttributes().get("role");
                    if (!"ADMIN".equals(role) && !"STAFF".equals(role)) throw new MessagingException("Chỉ nhân viên được theo dõi hộp thư");
                }
                return msg;
            }
        });
    }
}
