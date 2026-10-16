package com.ares.shop.web;

import com.ares.shop.model.User;
import com.ares.shop.repo.UserRepo;
import com.ares.shop.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.List;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final UserRepo users; private final PasswordEncoder enc; private final JwtService jwt;

    public AuthController(UserRepo users, PasswordEncoder enc, JwtService jwt) {
        this.users = users; this.enc = enc; this.jwt = jwt;
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

    @org.springframework.beans.factory.annotation.Autowired org.springframework.security.oauth2.client.registration.ClientRegistrationRepository regs;
    @org.springframework.beans.factory.annotation.Autowired com.ares.shop.service.SocialLoginService social;
    @org.springframework.beans.factory.annotation.Autowired com.ares.shop.service.OtpService otp;
    @org.springframework.beans.factory.annotation.Value("${app.oauth.redirect-base}") String redirectBase;
    boolean has(String p) { return regs.findByRegistrationId(p) != null; }

    /** Phương thức đăng nhập khả dụng (nút Google/Facebook ẩn nếu chưa cấu hình). */
    @GetMapping("/providers")
    public Map<String, Object> providers() { return Map.of("google", has("google"), "facebook", has("facebook"), "phone", true, "sms", otp.providerName()); }

    /** URL bắt đầu đăng nhập Google/Facebook (Spring OAuth2 Client); 501 nếu chưa cấu hình. */
    @GetMapping("/oauth/{provider}/url")
    public Map<String, String> oauthUrl(@PathVariable String provider) {
        if (!List.of("google", "facebook").contains(provider)) throw ApiException.bad("Nhà cung cấp không hỗ trợ");
        if (!has(provider)) throw new ApiException(HttpStatus.NOT_IMPLEMENTED, "Đăng nhập " + provider + " chưa được cấu hình");
        return Map.of("url", redirectBase.replaceAll("/+$", "") + com.ares.shop.security.SocialAuthConfig.AUTH_BASE + "/" + provider);
    }
    public record ExchangeReq(String code, String totp) {}
    /** Đổi mã một lần (từ /oauth-callback) lấy JWT. Tài khoản bật 2FA phải nhập TOTP. */
    @PostMapping("/oauth/exchange")
    public Map<String, Object> exchange(@RequestBody ExchangeReq r) {
        Long id = social.redeem(r.code()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Mã đăng nhập không hợp lệ hoặc đã hết hạn"));
        User u = users.findById(id).orElseThrow();
        return finish(u, r.totp(), () -> social.issue(u.id));
    }
    Map<String, Object> finish(User u, String totp, java.util.function.Supplier<String> retry) {
        if (u.locked) throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khoá");
        if (Boolean.TRUE.equals(u.totpEnabled)) {
            if (totp == null || totp.isBlank()) return Map.of("totpRequired", true, "retryCode", retry.get());
            if (!com.ares.shop.service.Totp.verify(u.totpSecret, totp)) throw new ApiException(HttpStatus.UNAUTHORIZED, "Mã xác thực 2 bước không đúng");
        }
        return Map.of("token", jwt.generate(u), "user", u);
    }

    public record OtpReq(String phone) {}
    public record OtpVerify(String phone, String code, String totp, String retryCode) {}
    @PostMapping("/otp/request")
    public Map<String, Object> otpRequest(@RequestBody OtpReq r, jakarta.servlet.http.HttpServletRequest rq) { return otp.request(r.phone(), rq.getRemoteAddr()); }
    /** Xác minh OTP → JWT. Nếu tài khoản bật 2FA: lần đầu trả retryCode, gửi lại kèm TOTP (không cần OTP mới). */
    @PostMapping("/otp/verify")
    public Map<String, Object> otpVerify(@RequestBody OtpVerify r) {
        User u = r.retryCode() != null ? users.findById(social.redeem(r.retryCode()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Phiên xác minh đã hết hạn"))).orElseThrow()
            : otp.verify(r.phone(), r.code());
        return finish(u, r.totp(), () -> social.issue(u.id));
    }

    @org.springframework.beans.factory.annotation.Autowired org.springframework.core.env.Environment env;
    /** Hộp thư SMS sandbox (chỉ với SMS mock và khi sandbox bật — để thử nghiệm/kiểm thử tự động). */
    @GetMapping("/sms/sandbox-outbox")
    public List<?> outbox(@RequestParam String phone) {
        if (!(otpSms instanceof com.ares.shop.service.SmsProviders.Mock m) || !env.getProperty("app.payments.sandbox.enabled", Boolean.class, true)) throw ApiException.notFound("Không có");
        String p = com.ares.shop.service.OtpService.normalize(phone); if (p == null) return List.of();
        return m.outbox.stream().filter(x -> x.to().equals("+84" + p.substring(1))).limit(5).toList();
    }
    @org.springframework.beans.factory.annotation.Autowired com.ares.shop.service.SmsProvider otpSms;
}
