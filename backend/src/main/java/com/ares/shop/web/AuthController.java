package com.ares.shop.web;

import com.ares.shop.model.User;
import com.ares.shop.repo.UserRepo;
import com.ares.shop.security.JwtService;
import com.ares.shop.service.OAuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final UserRepo users; private final PasswordEncoder enc; private final JwtService jwt; private final OAuthService oauth;

    public AuthController(UserRepo users, PasswordEncoder enc, JwtService jwt, OAuthService oauth) {
        this.users = users; this.enc = enc; this.jwt = jwt; this.oauth = oauth;
    }

    public record RegisterReq(@NotBlank @Email String email, @NotBlank @Size(min = 6, message = "tối thiểu 6 ký tự") String password,
                              @NotBlank String fullName, String phone) {}
    public record LoginReq(@NotBlank String email, @NotBlank String password, String totp) {}

    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterReq r) {
        if (users.existsByEmailIgnoreCase(r.email())) throw ApiException.bad("Email đã được sử dụng");
        User u = new User();
        u.email = r.email().trim().toLowerCase(); u.password = enc.encode(r.password());
        u.fullName = r.fullName(); u.phone = r.phone();
        users.save(u);
        return Map.of("token", jwt.generate(u), "user", u);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginReq r) {
        User u = users.findByEmailIgnoreCase(r.email().trim())
                .filter(x -> x.password != null && enc.matches(r.password(), x.password))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"));
        if (u.locked) throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khoá");
        if (Boolean.TRUE.equals(u.totpEnabled)) {
            if (r.totp() == null || r.totp().isBlank()) return Map.of("totpRequired", true);
            if (!com.ares.shop.service.Totp.verify(u.totpSecret, r.totp())) throw new ApiException(HttpStatus.UNAUTHORIZED, "Mã xác thực 2 bước không đúng");
        }
        return Map.of("token", jwt.generate(u), "user", u);
    }

    @GetMapping("/me")
    public User me() { return Current.user(); }

    /** Kiểm tra cấu hình và trả URL bắt đầu đăng nhập (backend) ; 501 nếu chưa cấu hình client id/secret. */
    @GetMapping("/oauth/{provider}/url")
    public Map<String, String> oauthUrl(@PathVariable String provider) {
        oauth.checkProvider(provider);
        return Map.of("url", oauth.startUrl(provider));
    }

    /** Bắt đầu: sinh state (+PKCE), đặt cookie HttpOnly gắn trình duyệt rồi chuyển tới Google/Facebook. */
    @GetMapping("/oauth/{provider}/start")
    public ResponseEntity<Void> start(@PathVariable String provider) {
        String target;
        ResponseCookie cookie = null;
        try {
            String[] r = oauth.begin(provider);
            cookie = ResponseCookie.from(OAuthService.STATE_COOKIE, r[0]).httpOnly(true).secure(oauth.secureCookie())
                    .sameSite("Lax").path("/api/auth/oauth").maxAge(600).build();
            target = r[1];
        } catch (ApiException e) {
            target = oauth.frontend() + "/login?error=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
        var b = ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target));
        if (cookie != null) b.header(HttpHeaders.SET_COOKIE, cookie.toString());
        return b.build();
    }

    /** Callback từ Google/Facebook: kiểm state, đổi code phía server, rồi chuyển về FRONTEND_URL/oauth-callback?ticket=... */
    @GetMapping("/oauth/{provider}/callback")
    public ResponseEntity<Void> callback(@PathVariable String provider, @RequestParam(required = false) String code,
                                         @RequestParam(required = false) String state, @RequestParam(required = false) String error,
                                         @CookieValue(name = OAuthService.STATE_COOKIE, required = false) String cookieState) {
        String target = oauth.handleCallback(provider, code, state, cookieState, error);
        ResponseCookie clear = ResponseCookie.from(OAuthService.STATE_COOKIE, "").httpOnly(true).secure(oauth.secureCookie())
                .sameSite("Lax").path("/api/auth/oauth").maxAge(0).build();
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).header(HttpHeaders.SET_COOKIE, clear.toString()).build();
    }

    public record TicketReq(String ticket) {}
    public record CompleteReq(String pending, String email) {}

    /** Đổi ticket dùng 1 lần lấy JWT (không để JWT trên URL). */
    @PostMapping("/oauth/exchange")
    public Map<String, Object> exchange(@RequestBody TicketReq r) { return oauth.exchange(r.ticket()); }

    /** Facebook không trả email: người dùng nhập email để tạo tài khoản. */
    @PostMapping("/oauth/complete")
    public Map<String, Object> complete(@RequestBody CompleteReq r) { return oauth.completeSignup(r.pending(), r.email()); }
}
