package com.ares.shop.service;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.time.*;
import java.util.*;

/**
 * Web Push chuẩn (RFC 8030 + VAPID RFC 8292 + mã hoá aes128gcm RFC 8291) chỉ dùng JDK — không thư viện ngoài.
 * Khoá VAPID sinh lần đầu và lưu trong AppSetting "push.vapid".
 */
@Service
public class PushService {
    static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding(); static final Base64.Decoder D64 = Base64.getUrlDecoder();
    /** Chỉ cho gửi tới dịch vụ push thật (chống SSRF); sandbox thêm hộp thư nội bộ để kiểm thử. */
    static final List<String> ALLOWED = List.of("fcm.googleapis.com", "updates.push.services.mozilla.com", "push.services.mozilla.com", "web.push.apple.com", ".notify.windows.com");
    private final SettingRepo settings; private final PushSubRepo subs; private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final boolean sandbox; private final int port;
    private ECPrivateKey vapidPriv; private byte[] vapidPub; private ECParameterSpec params;

    public PushService(SettingRepo settings, PushSubRepo subs, @Value("${app.payments.sandbox.enabled:true}") boolean sandbox, @Value("${server.port:8080}") int port) {
        this.settings = settings; this.subs = subs; this.sandbox = sandbox; this.port = port;
    }

    private synchronized void keys() {
        if (vapidPriv != null) return;
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("EC"); g.initialize(new ECGenParameterSpec("secp256r1"));
            params = ((ECPublicKey) g.generateKeyPair().getPublic()).getParams();
            var st = settings.findById("push.vapid").orElse(null);
            if (st != null) {
                String[] p = st.v.split("\\|");
                vapidPriv = (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(p[0])));
                vapidPub = D64.decode(p[1]);
            } else {
                KeyPair kp = g.generateKeyPair(); vapidPriv = (ECPrivateKey) kp.getPrivate(); vapidPub = raw((ECPublicKey) kp.getPublic());
                AppSetting s = new AppSetting(); s.k = "push.vapid"; s.v = Base64.getEncoder().encodeToString(vapidPriv.getEncoded()) + "|" + B64.encodeToString(vapidPub); settings.save(s);
            }
        } catch (Exception e) { throw new IllegalStateException("Không khởi tạo được khoá VAPID", e); }
    }
    public String publicKey() { keys(); return B64.encodeToString(vapidPub); }

    static byte[] pad32(BigInteger v) { byte[] b = v.toByteArray(), o = new byte[32]; System.arraycopy(b, Math.max(0, b.length - 32), o, Math.max(0, 32 - b.length), Math.min(32, b.length)); return o; }
    static byte[] raw(ECPublicKey k) { byte[] o = new byte[65]; o[0] = 4; System.arraycopy(pad32(k.getW().getAffineX()), 0, o, 1, 32); System.arraycopy(pad32(k.getW().getAffineY()), 0, o, 33, 32); return o; }
    ECPublicKey fromRaw(byte[] r) throws Exception {
        if (r.length != 65 || r[0] != 4) throw new IllegalArgumentException("Khoá p256dh không hợp lệ");
        var pt = new ECPoint(new BigInteger(1, Arrays.copyOfRange(r, 1, 33)), new BigInteger(1, Arrays.copyOfRange(r, 33, 65)));
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(pt, params));
    }
    static byte[] hmac(byte[] key, byte[]... parts) throws Exception { Mac m = Mac.getInstance("HmacSHA256"); m.init(new SecretKeySpec(key, "HmacSHA256")); for (byte[] p : parts) m.update(p); return m.doFinal(); }
    static byte[] cat(byte[]... a) { int n = 0; for (byte[] x : a) n += x.length; ByteBuffer b = ByteBuffer.allocate(n); for (byte[] x : a) b.put(x); return b.array(); }

    /** Mã hoá payload theo RFC 8291 (aes128gcm, 1 bản ghi). */
    byte[] encrypt(byte[] plain, byte[] uaPub, byte[] authSecret) throws Exception {
        keys();
        KeyPairGenerator g = KeyPairGenerator.getInstance("EC"); g.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair as = g.generateKeyPair(); byte[] asPub = raw((ECPublicKey) as.getPublic());
        KeyAgreement ka = KeyAgreement.getInstance("ECDH"); ka.init(as.getPrivate()); ka.doPhase(fromRaw(uaPub), true); byte[] shared = ka.generateSecret();
        byte[] prkKey = hmac(authSecret, shared);
        byte[] ikm = hmac(prkKey, "WebPush: info\0".getBytes(StandardCharsets.US_ASCII), uaPub, asPub, new byte[]{1});
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        byte[] prk = hmac(salt, ikm);
        byte[] cek = Arrays.copyOf(hmac(prk, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), new byte[]{1}), 16);
        byte[] nonce = Arrays.copyOf(hmac(prk, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), new byte[]{1}), 12);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] ct = c.doFinal(cat(plain, new byte[]{2}));
        return cat(salt, ByteBuffer.allocate(4).putInt(4096).array(), new byte[]{65}, asPub, ct);
    }
    String vapidJwt(String audience) throws Exception {
        keys();
        String h = B64.encodeToString("{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));
        String p = B64.encodeToString(json.writeValueAsBytes(Map.of("aud", audience, "exp", Instant.now().plusSeconds(12 * 3600).getEpochSecond(), "sub", "mailto:support@arestoys.vn")));
        Signature s = Signature.getInstance("SHA256withECDSAinP1363Format"); s.initSign(vapidPriv); s.update((h + "." + p).getBytes(StandardCharsets.US_ASCII));
        return h + "." + p + "." + B64.encodeToString(s.sign());
    }

    public boolean endpointAllowed(String endpoint) {
        try {
            URI u = URI.create(endpoint); String host = u.getHost(); if (host == null) return false;
            if (sandbox && "http".equals(u.getScheme()) && (host.equals("localhost") || host.equals("127.0.0.1")) && u.getPort() == port && u.getPath().startsWith("/api/public/push/sandbox-inbox/")) return true;
            if (!"https".equals(u.getScheme())) return false;
            return ALLOWED.stream().anyMatch(a -> a.startsWith(".") ? host.endsWith(a) : host.equals(a));
        } catch (Exception e) { return false; }
    }

    /** Gửi 1 thông báo tới 1 đăng ký. 404/410 → xoá đăng ký hết hạn. Trả về HTTP status (hoặc -1 nếu lỗi mạng). */
    public int send(PushSub s, Map<String, Object> payload) {
        try {
            if (!endpointAllowed(s.endpoint)) { s.lastStatus = "BLOCKED"; subs.save(s); return -1; }
            byte[] body = encrypt(json.writeValueAsBytes(payload), D64.decode(s.p256dh), D64.decode(s.auth));
            URI u = URI.create(s.endpoint); String aud = u.getScheme() + "://" + u.getHost() + (u.getPort() > 0 ? ":" + u.getPort() : "");
            HttpRequest rq = HttpRequest.newBuilder(u).timeout(Duration.ofSeconds(8))
                .header("TTL", "86400").header("Urgency", "normal").header("Content-Encoding", "aes128gcm").header("Content-Type", "application/octet-stream")
                .header("Authorization", "vapid t=" + vapidJwt(aud) + ", k=" + publicKey())
                .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
            int code = http.send(rq, HttpResponse.BodyHandlers.discarding()).statusCode();
            s.lastSentAt = LocalDateTime.now(); s.lastStatus = String.valueOf(code);
            if (code == 404 || code == 410) { subs.delete(s); return code; }
            if (code >= 200 && code < 300) { s.sent++; s.failures = 0; } else s.failures++;
            subs.save(s); return code;
        } catch (Exception e) { s.failures++; s.lastStatus = "ERR " + e.getClass().getSimpleName(); subs.save(s); return -1; }
    }
    public int sendToUser(Long userId, String title, String body, String link) {
        int ok = 0; if (userId == null) return 0;
        for (PushSub s : subs.findByUserId(userId)) { int c = send(s, Map.of("title", title, "body", body == null ? "" : body, "url", link == null ? "/" : link, "tag", "ares-" + System.currentTimeMillis())); if (c >= 200 && c < 300) ok++; }
        return ok;
    }
    private final java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2, r -> { Thread t = new Thread(r, "web-push"); t.setDaemon(true); return t; });
    /** Gửi nền (không chặn luồng nghiệp vụ). Bỏ qua nếu người dùng chưa đăng ký push. */
    public void sendToUserAsync(Long userId, String title, String body, String link) { if (userId == null || subs.findByUserId(userId).isEmpty()) return; pool.submit(() -> { try { sendToUser(userId, title, body, link); } catch (Exception ignored) {} }); }
}
