package com.ares.shop.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

@Configuration
public class SmsProviders {
    /** Mock: không gửi thật, lưu 50 tin gần nhất để xem ở hộp thư sandbox (chỉ khi sandbox bật). */
    public static class Mock implements SmsProvider {
        public record Msg(String to, String text, LocalDateTime at) {}
        public final Deque<Msg> outbox = new ConcurrentLinkedDeque<>();
        public String name() { return "mock"; }
        public void send(String to, String text) { outbox.addFirst(new Msg(to, text, LocalDateTime.now())); while (outbox.size() > 50) outbox.removeLast(); }
    }
    /** Twilio Messages API (Basic auth). */
    public static class Twilio implements SmsProvider {
        final String sid, token, from; final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
        Twilio(String sid, String token, String from) { this.sid = sid; this.token = token; this.from = from; }
        public String name() { return "twilio"; }
        public void send(String to, String text) throws Exception {
            String body = "To=" + URLEncoder.encode(to, StandardCharsets.UTF_8) + "&From=" + URLEncoder.encode(from, StandardCharsets.UTF_8) + "&Body=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
            var r = http.send(HttpRequest.newBuilder(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + sid + "/Messages.json")).timeout(Duration.ofSeconds(15))
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((sid + ":" + token).getBytes(StandardCharsets.UTF_8)))
                .header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() / 100 != 2) throw new IllegalStateException("Twilio HTTP " + r.statusCode());
        }
    }
    @Bean
    public SmsProvider smsProvider(@Value("${app.sms.provider:mock}") String p, @Value("${app.sms.twilio.account-sid:}") String sid,
                                   @Value("${app.sms.twilio.auth-token:}") String token, @Value("${app.sms.twilio.from:}") String from) {
        if ("twilio".equalsIgnoreCase(p) && !sid.isBlank() && !token.isBlank() && !from.isBlank()) return new Twilio(sid, token, from);
        return new Mock();
    }
}
