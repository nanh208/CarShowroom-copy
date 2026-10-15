package com.ares.shop.service;
import javax.crypto.Mac; import javax.crypto.spec.SecretKeySpec; import java.security.SecureRandom;
/** TOTP RFC 6238 (SHA1, 30s, 6 số) — dùng với Google Authenticator / Authy. */
public final class Totp {
    private static final String B32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    public static String newSecret() { byte[] b = new byte[20]; new SecureRandom().nextBytes(b); return base32(b); }
    static String base32(byte[] d) { StringBuilder s = new StringBuilder(); int buf = 0, bits = 0;
        for (byte x : d) { buf = (buf << 8) | (x & 0xff); bits += 8; while (bits >= 5) { s.append(B32.charAt((buf >> (bits - 5)) & 31)); bits -= 5; } }
        if (bits > 0) s.append(B32.charAt((buf << (5 - bits)) & 31)); return s.toString(); }
    static byte[] unbase32(String s) { s = s.replace("=", "").toUpperCase(); var out = new java.io.ByteArrayOutputStream(); int buf = 0, bits = 0;
        for (char c : s.toCharArray()) { int v = B32.indexOf(c); if (v < 0) continue; buf = (buf << 5) | v; bits += 5; if (bits >= 8) { out.write((buf >> (bits - 8)) & 0xff); bits -= 8; } }
        return out.toByteArray(); }
    public static String code(String secret, long step) {
        try { Mac m = Mac.getInstance("HmacSHA1"); m.init(new SecretKeySpec(unbase32(secret), "HmacSHA1"));
            byte[] h = m.doFinal(java.nio.ByteBuffer.allocate(8).putLong(step).array()); int o = h[h.length - 1] & 0xf;
            int bin = ((h[o] & 0x7f) << 24) | ((h[o + 1] & 0xff) << 16) | ((h[o + 2] & 0xff) << 8) | (h[o + 3] & 0xff);
            return String.format("%06d", bin % 1_000_000);
        } catch (Exception e) { throw new IllegalStateException(e); } }
    public static boolean verify(String secret, String code) {
        if (secret == null || code == null) return false; String c = code.replaceAll("\\s", ""); long t = System.currentTimeMillis() / 30_000;
        for (long d = -1; d <= 1; d++) if (code(secret, t + d).equals(c)) return true; return false; }
}
