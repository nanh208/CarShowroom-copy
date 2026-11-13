package com.ares.shop.service;

import com.ares.shop.model.User;
import com.ares.shop.repo.UserRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class OAuthServiceTest {
    @Autowired OAuthService oauth;
    @Autowired UserRepo users;

    static Map<String, String> query(String url) {
        Map<String, String> m = new HashMap<>();
        String q = URI.create(url).getRawQuery();
        for (String kv : q.split("&")) { int i = kv.indexOf('='); m.put(kv.substring(0, i), URLDecoder.decode(kv.substring(i + 1), StandardCharsets.UTF_8)); }
        return m;
    }

    @Test void beginBuildsStateAndPkceForGoogle() {
        String[] r = oauth.begin("google");
        Map<String, String> q = query(r[1]);
        assertEquals(r[0], q.get("state"));
        assertEquals("S256", q.get("code_challenge_method"));
        assertNotNull(q.get("code_challenge"));
        assertEquals("http://localhost:8080/api/auth/oauth/google/callback", q.get("redirect_uri"));
        assertTrue(query(oauth.begin("facebook")[1]).containsKey("state"));
    }

    @Test void pkceChallengeRfc7636Vector() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", OAuthService.pkceChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"));
    }

    @Test void callbackRejectsBadOrMissingState() {
        String[] r = oauth.begin("google");
        assertTrue(oauth.handleCallback("google", "code", r[0], "other", null).contains("/login?error="));
        assertTrue(oauth.handleCallback("google", "code", r[0], null, null).contains("/login?error="));
        assertTrue(oauth.handleCallback("google", "code", "forged", "forged", null).contains("/login?error="));
        String[] f = oauth.begin("facebook"); // state của provider khác
        assertTrue(oauth.handleCallback("google", "code", f[0], f[0], null).contains("/login?error="));
    }

    @Test void googleVerifiedEmailLinksExistingAccount() {
        User u = new User(); u.email = "linkme@example.com"; u.fullName = "Link"; users.save(u);
        String q = oauth.resolve(new OAuthService.Profile("google", "g-123", "LinkMe@example.com", true, "Link", null));
        assertTrue(q.startsWith("ticket="));
        Map<String, Object> res = oauth.exchange(URLDecoder.decode(q.substring(7), StandardCharsets.UTF_8));
        assertNotNull(res.get("token"));
        assertEquals("g-123", users.findByEmailIgnoreCase("linkme@example.com").orElseThrow().googleId);
        // ticket chỉ dùng 1 lần
        assertThrows(RuntimeException.class, () -> oauth.exchange(URLDecoder.decode(q.substring(7), StandardCharsets.UTF_8)));
        // lần sau tìm theo googleId kể cả khi email đổi
        assertTrue(oauth.resolve(new OAuthService.Profile("google", "g-123", "new@example.com", true, "Link", null)).startsWith("ticket="));
        assertFalse(users.existsByEmailIgnoreCase("new@example.com"));
    }

    @Test void unverifiedEmailDoesNotLink() {
        User u = new User(); u.email = "victim@example.com"; users.save(u);
        String q = oauth.resolve(new OAuthService.Profile("google", "g-evil", "victim@example.com", false, "Evil", null));
        assertTrue(q.startsWith("pending="), "không tự liên kết khi email chưa xác minh");
        assertNull(users.findByEmailIgnoreCase("victim@example.com").orElseThrow().googleId);
    }

    @Test void facebookWithoutEmailAsksForEmailAndNeverFabricates() {
        String q = oauth.resolve(new OAuthService.Profile("facebook", "fb-999", null, false, "No Mail", null));
        assertTrue(q.startsWith("pending="));
        String key = URLDecoder.decode(q.substring(8, q.indexOf('&')), StandardCharsets.UTF_8);
        assertTrue(users.findAll().stream().noneMatch(x -> x.email.endsWith("@facebook.local")));
        assertThrows(RuntimeException.class, () -> oauth.completeSignup(key, "not-an-email"));
        User existing = new User(); existing.email = "taken@example.com"; users.save(existing);
        assertThrows(RuntimeException.class, () -> oauth.completeSignup(key, "taken@example.com"));
        Map<String, Object> res = oauth.completeSignup(key, "real@example.com");
        assertNotNull(res.get("token"));
        User created = users.findByFacebookId("fb-999").orElseThrow();
        assertEquals("real@example.com", created.email);
        assertEquals("FACEBOOK", created.provider);
        // lần sau đăng nhập thẳng bằng facebookId
        assertTrue(oauth.resolve(new OAuthService.Profile("facebook", "fb-999", null, false, "No Mail", null)).startsWith("ticket="));
    }
}
