package com.ares.shop.security;

import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Spring Security OAuth2 Client cho Google/Facebook. Chỉ đăng ký nhà cung cấp có đủ client id + secret trong env;
 * thiếu → nút đăng nhập ẩn/khoá, endpoint trả 501. Trạng thái (state) lưu trong cookie HttpOnly có ký HMAC (backend stateless).
 */
@Configuration
public class SocialAuthConfig {
    public static final String AUTH_BASE = "/api/auth/oauth2/authorize", CALLBACK = "/api/auth/oauth2/callback/*";

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(Environment env, @Value("${app.oauth.redirect-base}") String base) {
        Map<String, ClientRegistration> m = new LinkedHashMap<>();
        for (String p : List.of("google", "facebook")) {
            String id = env.getProperty("app.oauth." + p + ".client-id", "").trim(), secret = env.getProperty("app.oauth." + p + ".client-secret", "").trim();
            if (id.isEmpty() || secret.isEmpty()) continue;
            CommonOAuth2Provider prov = p.equals("google") ? CommonOAuth2Provider.GOOGLE : CommonOAuth2Provider.FACEBOOK;
            var b = prov.getBuilder(p).clientId(id).clientSecret(secret).redirectUri(base.replaceAll("/+$", "") + "/api/auth/oauth2/callback/{registrationId}");
            if (p.equals("google")) b.scope("openid", "email", "profile"); else b.scope("email", "public_profile");
            m.put(p, b.build());
        }
        return new Repo(m);
    }
    /** Cho phép danh sách rỗng (InMemoryClientRegistrationRepository bắt buộc ≥1). */
    public record Repo(Map<String, ClientRegistration> regs) implements ClientRegistrationRepository {
        public ClientRegistration findByRegistrationId(String id) { return regs.get(id); }
        public boolean has(String id) { return regs.containsKey(id); }
    }

    /** Lưu OAuth2AuthorizationRequest trong cookie đã ký (10 phút). */
    public static class CookieAuthRequestRepo implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
        static final String NAME = "ares_oauth2_req"; private final byte[] key;
        public CookieAuthRequestRepo(String secret) { this.key = secret.getBytes(StandardCharsets.UTF_8); }
        String mac(String v) { try { Mac m = Mac.getInstance("HmacSHA256"); m.init(new SecretKeySpec(key, "HmacSHA256")); return Base64.getUrlEncoder().withoutPadding().encodeToString(m.doFinal(v.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
        public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest rq) {
            if (rq.getCookies() == null) return null;
            for (Cookie c : rq.getCookies()) if (NAME.equals(c.getName())) {
                String[] parts = c.getValue().split("\\.");
                if (parts.length != 2 || !MessageDigest.isEqual(mac(parts[0]).getBytes(), parts[1].getBytes())) return null;
                try (var in = new ObjectInputStream(new ByteArrayInputStream(Base64.getUrlDecoder().decode(parts[0])))) {
                    Object o = in.readObject(); return o instanceof OAuth2AuthorizationRequest r ? r : null;
                } catch (Exception e) { return null; }
            }
            return null;
        }
        public void saveAuthorizationRequest(OAuth2AuthorizationRequest r, HttpServletRequest rq, HttpServletResponse rs) {
            if (r == null) { clear(rs); return; }
            try { var bo = new ByteArrayOutputStream(); try (var out = new ObjectOutputStream(bo)) { out.writeObject(r); }
                String v = Base64.getUrlEncoder().withoutPadding().encodeToString(bo.toByteArray());
                Cookie c = new Cookie(NAME, v + "." + mac(v)); c.setHttpOnly(true); c.setPath("/api/auth/oauth2"); c.setMaxAge(600); c.setSecure(rq.isSecure()); c.setAttribute("SameSite", "Lax"); rs.addCookie(c);
            } catch (IOException e) { throw new UncheckedIOException(e); }
        }
        public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest rq, HttpServletResponse rs) { var r = loadAuthorizationRequest(rq); clear(rs); return r; }
        static void clear(HttpServletResponse rs) { Cookie c = new Cookie(NAME, ""); c.setPath("/api/auth/oauth2"); c.setMaxAge(0); c.setHttpOnly(true); rs.addCookie(c); }
    }

    /** Không giữ access token của Google/Facebook sau khi đăng nhập (chỉ cần hồ sơ). */
    public static class NoStoreClients implements org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository {
        public <T extends org.springframework.security.oauth2.client.OAuth2AuthorizedClient> T loadAuthorizedClient(String id, org.springframework.security.core.Authentication a, HttpServletRequest rq) { return null; }
        public void saveAuthorizedClient(org.springframework.security.oauth2.client.OAuth2AuthorizedClient c, org.springframework.security.core.Authentication a, HttpServletRequest rq, HttpServletResponse rs) { }
        public void removeAuthorizedClient(String id, org.springframework.security.core.Authentication a, HttpServletRequest rq, HttpServletResponse rs) { }
    }

    /** Nhà cung cấp chưa cấu hình → bỏ qua (404) thay vì lỗi 500. */
    public static org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver safeResolver(ClientRegistrationRepository regs) {
        var d = new org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver(regs, AUTH_BASE);
        return new org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver() {
            public OAuth2AuthorizationRequest resolve(HttpServletRequest rq) { try { return d.resolve(rq); } catch (IllegalArgumentException e) { return null; } }
            public OAuth2AuthorizationRequest resolve(HttpServletRequest rq, String id) { return regs.findByRegistrationId(id) == null ? null : d.resolve(rq, id); }
        };
    }
}
