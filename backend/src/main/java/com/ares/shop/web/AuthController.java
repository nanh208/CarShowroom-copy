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
    public record LoginReq(@NotBlank String email, @NotBlank String password) {}

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
        return Map.of("token", jwt.generate(u), "user", u);
    }

    @GetMapping("/me")
    public User me() { return Current.user(); }

    /** Trả về URL chuyển hướng tới Google/Facebook; 501 nếu chưa cấu hình client id. */
    @GetMapping("/oauth/{provider}/url")
    public Map<String, String> oauthUrl(@PathVariable String provider) {
        return Map.of("url", oauth.authorizeUrl(provider));
    }

    /** Callback từ nhà cung cấp OAuth2 -> phát JWT -> chuyển về frontend /oauth-callback?token=... */
    @GetMapping("/oauth/{provider}/callback")
    public ResponseEntity<Void> callback(@PathVariable String provider, @RequestParam(required = false) String code) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(oauth.handleCallback(provider, code))).build();
    }
}
