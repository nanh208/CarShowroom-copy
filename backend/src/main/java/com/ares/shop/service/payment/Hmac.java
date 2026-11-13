package com.ares.shop.service.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Tiện ích ký/so chữ ký HMAC dùng chung cho MoMo, VNPay, payOS, Casso. */
public final class Hmac {
    private Hmac() {}

    public static String sha256Hex(String key, String data) { return hex("HmacSHA256", key, data); }
    public static String sha512Hex(String key, String data) { return hex("HmacSHA512", key, data); }

    private static String hex(String alg, String key, String data) {
        try {
            Mac mac = Mac.getInstance(alg);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), alg));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC error", e);
        }
    }

    /** So sánh hằng thời gian (chống timing attack), không phân biệt hoa thường cho chuỗi hex. */
    public static boolean equalsHex(String expected, String actual) {
        if (expected == null || actual == null) return false;
        return MessageDigest.isEqual(expected.toLowerCase().getBytes(StandardCharsets.UTF_8),
                actual.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    /** So sánh hằng thời gian cho token/API key. */
    public static boolean equalsSecret(String expected, String actual) {
        if (expected == null || expected.isBlank() || actual == null) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.trim().getBytes(StandardCharsets.UTF_8));
    }
}
