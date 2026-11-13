package com.ares.shop.service;

import com.ares.shop.model.User;
import com.ares.shop.repo.UserRepo;
import com.ares.shop.security.JwtService;
import com.ares.shop.service.payment.Hmac;
import com.ares.shop.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Đăng nhập Google / Facebook theo luồng Authorization Code (đổi code phía server).
 * - state ngẫu nhiên lưu phía server + cookie HttpOnly gắn với trình duyệt (chống CSRF/login-fixation).
 * - Google dùng thêm PKCE (S256).
 * - Định danh bằng googleId/facebookId; chỉ liên kết theo email khi email đã được nhà cung cấp xác minh.
 * - Facebook không trả email => KHÔNG tạo email giả; yêu cầu người dùng nhập email trước khi tạo tài khoản.
 * - JWT không đặt trên URL: frontend nhận "ticket" dùng 1 lần rồi đổi lấy JWT qua POST.
 * Lưu ý: kho state/ticket nằm trong bộ nhớ => chạy 1 instance (hoặc chuyển sang Redis/DB khi scale).
 */
@Service
public class OAuthService {
    private static final Logger log = LoggerFactory.getLogger(OAuthService.class);
    public static final String STATE_COOKIE = "ares_oauth_state";
    static final long STATE_TTL_MS = 10 * 60_000L, TICKET_TTL_MS = 2 * 60_000L, PENDING_TTL_MS = 15 * 60_000L;

    private final Environment env; private final UserRepo users; private final JwtService jwt;
    private final RestClient http;
    private final SecureRandom rnd = new SecureRandom();

    record PendingState(String provider, String verifier, long exp) {}
    record Ticket(Long userId, long exp) {}
    /** Hồ sơ mạng xã hội đã xác thực nhưng thiếu email. */
    public record Profile(String provider, String providerId, String email, boolean emailVerified, String name, String avatar) {}
    record PendingSignup(Profile profile, long exp) {}

    final Map<String, PendingState> states = new ConcurrentHashMap<>();
    final Map<String, Ticket> tickets = new ConcurrentHashMap<>();
    final Map<String, PendingSignup> pendings = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public OAuthService(Environment env, UserRepo users, JwtService jwt) {
        this(env, users, jwt, RestClient.create());
    }

    OAuthService(Environment env, UserRepo users, JwtService jwt, RestClient http) {
        this.env = env; this.users = users; this.jwt = jwt; this.http = http;
    }

    // ---------------------------------------------------------------- cấu hình

    private String prop(String provider, String k) { return env.getProperty("app.oauth." + provider + "." + k, "").trim(); }
    private String redirectBase() {
        String b = env.getProperty("app.oauth.redirect-base", "http://localhost:8080");
        return b.endsWith("/") ? b.substring(0, b.length() - 1) : b;
    }
    public String frontend() { return env.getProperty("app.frontend-url", "http://localhost:5173"); }
    public String redirectUri(String provider) {
        String v = prop(provider, "redirect-uri");
        return v.isBlank() ? redirectBase() + "/api/auth/oauth/" + provider + "/callback" : v;
    }
    public String startUrl(String provider) { return redirectBase() + "/api/auth/oauth/" + provider + "/start"; }
    public boolean secureCookie() { return redirectBase().startsWith("https://"); }
    private String fbVersion() { String v = prop("facebook", "graph-version"); return v.isBlank() ? "v19.0" : v; }
    private static String enc(String s) { return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8); }

    public void checkProvider(String provider) {
        if (!"google".equals(provider) && !"facebook".equals(provider)) throw ApiException.bad("Nhà cung cấp không hỗ trợ");
        if (prop(provider, "client-id").isBlank() || prop(provider, "client-secret").isBlank())
            throw new ApiException(HttpStatus.NOT_IMPLEMENTED, "Đăng nhập " + provider + " chưa được cấu hình (thiếu client id/secret).");
    }

    private String random(int bytes) {
        byte[] b = new byte[bytes]; rnd.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    static String pkceChallenge(String verifier) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(d);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    private void purge() {
        long now = System.currentTimeMillis();
        states.values().removeIf(s -> s.exp() < now);
        tickets.values().removeIf(t -> t.exp() < now);
        pendings.values().removeIf(p -> p.exp() < now);
    }

    // ---------------------------------------------------------------- bước 1: chuyển tới nhà cung cấp

    /** Tạo state (+PKCE) và URL uỷ quyền. Trả [state, authorizeUrl]. */
    public String[] begin(String provider) {
        checkProvider(provider); purge();
        String state = random(32);
        String verifier = "google".equals(provider) ? random(48) : null;
        states.put(state, new PendingState(provider, verifier, System.currentTimeMillis() + STATE_TTL_MS));
        String id = prop(provider, "client-id");
        String url;
        if ("google".equals(provider)) {
            url = "https://accounts.google.com/o/oauth2/v2/auth?response_type=code&scope=" + enc("openid email profile")
                    + "&client_id=" + enc(id) + "&redirect_uri=" + enc(redirectUri(provider)) + "&state=" + enc(state)
                    + "&code_challenge=" + enc(pkceChallenge(verifier)) + "&code_challenge_method=S256&prompt=select_account";
        } else {
            url = "https://www.facebook.com/" + fbVersion() + "/dialog/oauth?response_type=code&scope=" + enc("email,public_profile")
                    + "&client_id=" + enc(id) + "&redirect_uri=" + enc(redirectUri(provider)) + "&state=" + enc(state);
        }
        return new String[]{state, url};
    }

    // ---------------------------------------------------------------- bước 2: callback

    /** Xử lý callback, trả URL frontend để chuyển hướng. */
    public String handleCallback(String provider, String code, String state, String cookieState, String error) {
        try {
            if (error != null && !error.isBlank()) return fail(provider, "Bạn đã huỷ hoặc nhà cung cấp từ chối đăng nhập");
            if (state == null || cookieState == null || !Hmac.equalsSecret(state, cookieState)) return fail(provider, "Phiên đăng nhập không hợp lệ (state)");
            PendingState ps = states.remove(state);
            if (ps == null || ps.exp() < System.currentTimeMillis() || !ps.provider().equals(provider)) return fail(provider, "Phiên đăng nhập đã hết hạn, vui lòng thử lại");
            if (code == null || code.isBlank()) return fail(provider, "Thiếu mã code");
            checkProvider(provider);

            Profile p = "google".equals(provider) ? googleProfile(code, ps.verifier()) : facebookProfile(code);
            return frontend() + "/oauth-callback?" + resolve(p);
        } catch (ApiException e) {
            return fail(provider, e.getMessage());
        } catch (Exception e) {
            log.warn("OAuth {} callback lỗi: {}", provider, e.toString());
            return fail(provider, "Đăng nhập " + provider + " thất bại");
        }
    }

    private String fail(String provider, String msg) { return frontend() + "/login?error=" + enc(msg); }

    @SuppressWarnings("unchecked")
    Profile googleProfile(String code, String verifier) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("code", code); form.add("client_id", prop("google", "client-id"));
        form.add("client_secret", prop("google", "client-secret"));
        form.add("redirect_uri", redirectUri("google")); form.add("grant_type", "authorization_code");
        form.add("code_verifier", verifier);
        Map<String, Object> tok = http.post().uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(Map.class);
        Object at = tok == null ? null : tok.get("access_token");
        if (at == null) throw ApiException.bad("Google không trả access token");
        Map<String, Object> info = http.get().uri("https://openidconnect.googleapis.com/v1/userinfo")
                .header("Authorization", "Bearer " + at).retrieve().body(Map.class);
        if (info == null || info.get("sub") == null) throw ApiException.bad("Không đọc được hồ sơ Google");
        boolean verified = Boolean.TRUE.equals(info.get("email_verified")) || "true".equals(String.valueOf(info.get("email_verified")));
        return new Profile("google", String.valueOf(info.get("sub")), (String) info.get("email"), verified,
                (String) info.get("name"), (String) info.get("picture"));
    }

    @SuppressWarnings("unchecked")
    Profile facebookProfile(String code) {
        String v = fbVersion();
        Map<String, Object> tok = http.get().uri("https://graph.facebook.com/" + v + "/oauth/access_token?client_id="
                + enc(prop("facebook", "client-id")) + "&client_secret=" + enc(prop("facebook", "client-secret"))
                + "&redirect_uri=" + enc(redirectUri("facebook")) + "&code=" + enc(code)).retrieve().body(Map.class);
        Object at = tok == null ? null : tok.get("access_token");
        if (at == null) throw ApiException.bad("Facebook không trả access token");
        String proof = Hmac.sha256Hex(prop("facebook", "client-secret"), String.valueOf(at));
        Map<String, Object> info = http.get().uri("https://graph.facebook.com/" + v + "/me?fields=id,name,email,picture.type(large)&access_token="
                + enc(String.valueOf(at)) + "&appsecret_proof=" + proof).retrieve().body(Map.class);
        if (info == null || info.get("id") == null) throw ApiException.bad("Không đọc được hồ sơ Facebook");
        String avatar = null;
        if (info.get("picture") instanceof Map<?, ?> pic && pic.get("data") instanceof Map<?, ?> d) avatar = (String) d.get("url");
        String email = (String) info.get("email");
        // Email Facebook trả về qua Graph API là email đã xác minh với Facebook
        return new Profile("facebook", String.valueOf(info.get("id")), email, email != null && !email.isBlank(), (String) info.get("name"), avatar);
    }

    // ---------------------------------------------------------------- tạo / liên kết tài khoản

    /** Trả query string cho /oauth-callback: "ticket=..." hoặc "pending=...&provider=...&name=...". */
    @Transactional
    public String resolve(Profile p) {
        Optional<User> byId = "google".equals(p.provider()) ? users.findByGoogleId(p.providerId()) : users.findByFacebookId(p.providerId());
        User u = byId.orElse(null);
        if (u == null && p.email() != null && !p.email().isBlank() && p.emailVerified()) {
            u = users.findByEmailIgnoreCase(p.email().trim()).orElse(null);
            if (u != null) { link(u, p); u = users.save(u); }
        }
        if (u == null) {
            if (p.email() == null || p.email().isBlank() || !p.emailVerified()) {
                String key = random(24);
                pendings.put(key, new PendingSignup(p, System.currentTimeMillis() + PENDING_TTL_MS));
                return "pending=" + enc(key) + "&provider=" + enc(p.provider()) + "&name=" + enc(p.name() == null ? "" : p.name());
            }
            u = create(p, p.email().trim().toLowerCase(Locale.ROOT));
        }
        return "ticket=" + enc(issueTicket(u));
    }

    private void link(User u, Profile p) {
        if ("google".equals(p.provider())) u.googleId = p.providerId(); else u.facebookId = p.providerId();
        if (u.avatarUrl == null || u.avatarUrl.isBlank()) u.avatarUrl = p.avatar();
        if (u.fullName == null || u.fullName.isBlank()) u.fullName = p.name();
    }

    private User create(Profile p, String email) {
        User n = new User(); n.email = email; n.fullName = p.name(); n.avatarUrl = p.avatar();
        n.provider = p.provider().toUpperCase(Locale.ROOT);
        link(n, p);
        return users.save(n);
    }

    private String issueTicket(User u) {
        if (u.locked) throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khoá");
        String t = random(32);
        tickets.put(t, new Ticket(u.id, System.currentTimeMillis() + TICKET_TTL_MS));
        return t;
    }

    /** Đổi ticket 1 lần lấy JWT. */
    public Map<String, Object> exchange(String ticket) {
        Ticket t = ticket == null ? null : tickets.remove(ticket);
        if (t == null || t.exp() < System.currentTimeMillis()) throw new ApiException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn, vui lòng thử lại");
        User u = users.findById(t.userId()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản"));
        if (u.locked) throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khoá");
        return Map.of("token", jwt.generate(u), "user", u);
    }

    /** Hoàn tất đăng ký khi nhà cung cấp không trả email: người dùng tự nhập email. */
    @Transactional
    public Map<String, Object> completeSignup(String pendingKey, String email) {
        PendingSignup ps = pendingKey == null ? null : pendings.get(pendingKey);
        if (ps == null || ps.exp() < System.currentTimeMillis()) throw new ApiException(HttpStatus.UNAUTHORIZED, "Phiên đăng ký đã hết hạn, vui lòng đăng nhập lại");
        String e = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!e.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw ApiException.bad("Email không hợp lệ");
        if (users.existsByEmailIgnoreCase(e))
            throw ApiException.bad("Email này đã có tài khoản. Hãy đăng nhập bằng email + mật khẩu (hoặc Google) — không thể tự liên kết khi chưa xác minh email.");
        Profile p = ps.profile();
        Optional<User> exists = "google".equals(p.provider()) ? users.findByGoogleId(p.providerId()) : users.findByFacebookId(p.providerId());
        User u = exists.orElseGet(() -> create(p, e));
        pendings.remove(pendingKey);
        if (u.locked) throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khoá");
        return Map.of("token", jwt.generate(u), "user", u);
    }
}
