package com.ares.shop.service;

import com.ares.shop.model.User;
import com.ares.shop.repo.UserRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Sau khi Google/Facebook xác thực: liên kết/tạo tài khoản và phát mã một lần (60s) để frontend đổi lấy JWT — không đặt JWT trên URL. */
@Service
public class SocialLoginService {
    private final UserRepo users; private final String frontend;
    record Pending(long userId, Instant exp) {}
    private final Map<String, Pending> codes = new ConcurrentHashMap<>(); private final SecureRandom rnd = new SecureRandom();
    public SocialLoginService(UserRepo users, @Value("${app.frontend-url}") String frontend) { this.users = users; this.frontend = frontend.replaceAll("/+$", ""); }
    public String frontend() { return frontend; }

    @Transactional
    public String onSuccess(OAuth2AuthenticationToken auth) {
        String provider = auth.getAuthorizedClientRegistrationId(); OAuth2User o = auth.getPrincipal(); Map<String, Object> a = o.getAttributes();
        String email = a.get("email") == null ? null : String.valueOf(a.get("email")).toLowerCase(Locale.ROOT), name = Objects.toString(a.get("name"), "");
        String avatar = "google".equals(provider) ? (String) a.get("picture") : null;
        String subject = String.valueOf("google".equals(provider) ? a.get("sub") : a.get("id"));
        // Google: chỉ tin email đã xác minh. Facebook chỉ trả email đã xác nhận; không có email → tài khoản riêng theo id, không liên kết.
        boolean verified = "google".equals(provider) ? Boolean.TRUE.equals(a.get("email_verified")) : email != null;
        if (email == null || !verified) email = provider + "_" + subject + "@social.ares.local";
        final String em = email;
        User u = users.findByEmailIgnoreCase(em).orElseGet(() -> { User n = new User(); n.email = em; n.fullName = name.isBlank() ? em : name; n.avatarUrl = avatar; n.provider = provider.toUpperCase(Locale.ROOT); return users.save(n); });
        if (u.locked) return frontend + "/login?error=" + java.net.URLEncoder.encode("Tài khoản đã bị khoá", java.nio.charset.StandardCharsets.UTF_8);
        return frontend + "/oauth-callback?code=" + issue(u.id);
    }
    public String issue(long userId) {
        codes.entrySet().removeIf(e -> e.getValue().exp().isBefore(Instant.now()));
        byte[] b = new byte[24]; rnd.nextBytes(b); String c = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        codes.put(c, new Pending(userId, Instant.now().plusSeconds(60))); return c;
    }
    /** Đổi mã một lần → userId (xoá ngay khi dùng). */
    public Optional<Long> redeem(String code) {
        if (code == null) return Optional.empty(); Pending p = codes.remove(code);
        return p == null || p.exp().isBefore(Instant.now()) ? Optional.empty() : Optional.of(p.userId());
    }
}
