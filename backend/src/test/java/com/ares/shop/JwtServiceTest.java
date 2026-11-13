package com.ares.shop;

import com.ares.shop.model.Enums.Role;
import com.ares.shop.model.User;
import com.ares.shop.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    @Test
    void testGenerateAndParseToken() {
        String secret = "test-secret-must-be-very-long-at-least-32-characters-12345";
        long expirationMs = 3600000; // 1 hour
        JwtService jwtService = new JwtService(secret, expirationMs);

        User user = new User();
        user.id = 42L;
        user.email = "test@ares.vn";
        user.role = Role.ADMIN;

        String token = jwtService.generate(user);
        assertNotNull(token);
        assertFalse(token.isBlank());

        Claims claims = jwtService.parse(token);
        assertEquals("test@ares.vn", claims.getSubject());
        assertEquals("ADMIN", claims.get("role"));
        assertEquals(42, ((Number) claims.get("uid")).longValue());
    }
}
