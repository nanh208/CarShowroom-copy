package com.ares.shop.service;

import com.ares.shop.model.User;
import com.ares.shop.repo.UserRepo;
import com.ares.shop.security.JwtService;
import com.ares.shop.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Đăng nhập Google / Facebook theo luồng Authorization Code. Cần cấu hình client id/secret (xem README). */
@Service
public class OAuthService {
    private final Environment env; private final UserRepo users; private final JwtService jwt;
    private final String redirectBase; private final String frontend;
    private final RestClient http = RestClient.create();

    public OAuthService(Environment env, UserRepo users, JwtService jwt,
                        @Value("${app.oauth.redirect-base}") String redirectBase, @Value("${app.frontend-url}") String frontend) {
        this.env = env; this.users = users; this.jwt = jwt; this.redirectBase = redirectBase; this.frontend = frontend;
    }

    private String prop(String provider, String k) { return env.getProperty("app.oauth." + provider + "." + k, ""); }
    private String redirect(String provider) { return redirectBase + "/api/auth/oauth/" + provider + "/callback"; }
    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    public String authorizeUrl(String provider) {
        String id = prop(provider, "client-id");
        if (!provider.equals("google") && !provider.equals("facebook")) throw ApiException.bad("Nhà cung cấp không hỗ trợ");
        if (id.isBlank()) throw new ApiException(HttpStatus.NOT_IMPLEMENTED,
                "Đăng nhập " + provider + " chưa được cấu hình (thiếu client id). Xem README.");
        if (provider.equals("google"))
            return "https://accounts.google.com/o/oauth2/v2/auth?response_type=code&scope=" + enc("openid email profile")
                    + "&client_id=" + enc(id) + "&redirect_uri=" + enc(redirect(provider));
        return "https://www.facebook.com/v19.0/dialog/oauth?scope=email,public_profile&client_id=" + enc(id)
                + "&redirect_uri=" + enc(redirect(provider));
    }

    @SuppressWarnings("unchecked")
    public String handleCallback(String provider, String code) {
        try {
            if (code == null) throw ApiException.bad("Thiếu mã code");
            String email, name, avatar = null;
            if (provider.equals("google")) {
                var form = new LinkedMultiValueMap<String, String>();
                form.add("code", code); form.add("client_id", prop(provider, "client-id"));
                form.add("client_secret", prop(provider, "client-secret"));
                form.add("redirect_uri", redirect(provider)); form.add("grant_type", "authorization_code");
                Map<String, Object> tok = http.post().uri("https://oauth2.googleapis.com/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(Map.class);
                Map<String, Object> info = http.get().uri("https://www.googleapis.com/oauth2/v3/userinfo")
                        .header("Authorization", "Bearer " + tok.get("access_token")).retrieve().body(Map.class);
                email = (String) info.get("email"); name = (String) info.get("name"); avatar = (String) info.get("picture");
            } else {
                Map<String, Object> tok = http.get().uri("https://graph.facebook.com/v19.0/oauth/access_token?client_id="
                        + enc(prop(provider, "client-id")) + "&client_secret=" + enc(prop(provider, "client-secret"))
                        + "&redirect_uri=" + enc(redirect(provider)) + "&code=" + enc(code)).retrieve().body(Map.class);
                Map<String, Object> info = http.get().uri("https://graph.facebook.com/me?fields=id,name,email&access_token="
                        + enc(String.valueOf(tok.get("access_token")))).retrieve().body(Map.class);
                name = (String) info.get("name");
                email = info.get("email") != null ? (String) info.get("email") : info.get("id") + "@facebook.local";
            }
            final String fName = name, fAvatar = avatar;
            User u = users.findByEmailIgnoreCase(email).orElseGet(() -> {
                User n = new User(); n.email = email.toLowerCase(); n.fullName = fName; n.avatarUrl = fAvatar;
                n.provider = provider.toUpperCase(); return users.save(n);
            });
            return frontend + "/oauth-callback?token=" + enc(jwt.generate(u));
        } catch (Exception e) {
            return frontend + "/login?error=" + enc("Đăng nhập " + provider + " thất bại");
        }
    }
}
