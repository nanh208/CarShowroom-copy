package com.ares.shop;

import com.ares.shop.service.Totp;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TotpTest {

    @Test
    void testNewSecretAndVerification() {
        String secret = Totp.newSecret();
        assertNotNull(secret);
        assertTrue(secret.length() >= 16);

        long currentStep = System.currentTimeMillis() / 30000;
        String code = Totp.code(secret, currentStep);
        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));

        assertTrue(Totp.verify(secret, code));
        assertFalse(Totp.verify(secret, "000000".equals(code) ? "999999" : "000000"));
        assertFalse(Totp.verify(null, code));
        assertFalse(Totp.verify(secret, null));
    }
}
