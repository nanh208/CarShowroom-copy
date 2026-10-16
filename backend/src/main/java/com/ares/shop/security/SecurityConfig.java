package com.ares.shop.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain chain(HttpSecurity http, JwtFilter jwtFilter, @Value("${app.frontend-url}") String frontend, @Value("${app.jwt.secret}") String secret,
                        com.ares.shop.service.SocialLoginService social,
                        org.springframework.security.oauth2.client.registration.ClientRegistrationRepository regs) throws Exception {
        http.csrf(c -> c.disable())
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))
            .cors(c -> c.configurationSource(cors(frontend)))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/**", "/api/public/**", "/api/chat/**", "/api/newsletter", "/ws", "/ws/**", "/h2-console/**", "/error", "/uploads/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/auctions/**", "/api/brands/**", "/api/promotions/**", "/api/options", "/api/categories", "/api/flash-sales", "/api/banners", "/api/shipping/**", "/api/warranty").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/promotions/check").permitAll()
                .requestMatchers("/api/admin/staff/**", "/api/admin/reports/**", "/api/admin/audit").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "STAFF")
                .anyRequest().authenticated())
            .oauth2Login(o -> o
                .authorizationEndpoint(e -> e.baseUri(SocialAuthConfig.AUTH_BASE).authorizationRequestRepository(new SocialAuthConfig.CookieAuthRequestRepo(secret)).authorizationRequestResolver(SocialAuthConfig.safeResolver(regs)))
                .redirectionEndpoint(r -> r.baseUri(SocialAuthConfig.CALLBACK))
                .authorizedClientRepository(new SocialAuthConfig.NoStoreClients())
                .successHandler((rq, rs, auth) -> rs.sendRedirect(social.onSuccess((org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) auth)))
                .failureHandler((rq, rs, ex) -> rs.sendRedirect(social.frontend() + "/login?error=" + java.net.URLEncoder.encode("Đăng nhập mạng xã hội thất bại", java.nio.charset.StandardCharsets.UTF_8))))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private CorsConfigurationSource cors(String frontend) {
        var cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(frontend, "http://localhost:5173", "http://localhost:4173"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        var src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
