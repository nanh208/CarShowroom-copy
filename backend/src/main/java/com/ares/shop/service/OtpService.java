package com.ares.shop.service;

import com.ares.shop.model.User;
import com.ares.shop.repo.UserRepo;
import com.ares.shop.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Đăng nhập bằng số điện thoại + mã OTP 6 số. Mã chỉ lưu dạng băm (SHA-256 + salt), hết hạn 5 phút, tối đa 5 lần nhập sai,
 * gửi lại sau 60 giây, tối đa 5 mã/giờ cho mỗi số và 20 mã/giờ cho mỗi IP.
 */
@Service
public class OtpService {
    record Otp(String hash, String salt, Instant exp, int[] attempts) {}
    private final Map<String, Otp> live = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> sentByPhone = new ConcurrentHashMap<>(), sentByIp = new ConcurrentHashMap<>();
    private final SecureRandom rnd = new SecureRandom(); private final SmsProvider sms; private final UserRepo users;
    public OtpService(SmsProvider sms, UserRepo users) { this.sms = sms; this.users = users; }
    public String providerName() { return sms.name(); }

    /** Chuẩn hoá SĐT Việt Nam: 0xxxxxxxxx / +84xxxxxxxxx → 0xxxxxxxxx. */
    public static String normalize(String raw) {
        if (raw == null) return null; String d = raw.replaceAll("[\\s.\\-()]", "");
        if (d.startsWith("+84")) d = "0" + d.substring(3); else if (d.startsWith("84") && d.length() == 11) d = "0" + d.substring(2);
        return d.matches("0[35789]\\d{8}") ? d : null;
    }
    static String sha(String s) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
    static boolean within(Map<String, Deque<Instant>> m, String k, int max, long secs) {
        Deque<Instant> q = m.computeIfAbsent(k, x -> new ArrayDeque<>()); Instant cut = Instant.now().minusSeconds(secs);
        synchronized (q) { while (!q.isEmpty() && q.peekFirst().isBefore(cut)) q.pollFirst(); return q.size() < max; }
    }
    static void mark(Map<String, Deque<Instant>> m, String k) { Deque<Instant> q = m.computeIfAbsent(k, x -> new ArrayDeque<>()); synchronized (q) { q.addLast(Instant.now()); } }

    public Map<String, Object> request(String rawPhone, String ip) {
        String phone = normalize(rawPhone); if (phone == null) throw ApiException.bad("Số điện thoại không hợp lệ");
        Otp cur = live.get(phone);
        if (cur != null && cur.exp().minusSeconds(240).isAfter(Instant.now())) throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Vui lòng đợi 60 giây trước khi gửi lại mã");
        if (!within(sentByPhone, phone, 5, 3600) || !within(sentByIp, ip, 20, 3600)) throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Bạn đã yêu cầu quá nhiều mã, thử lại sau");
        String code = String.format("%06d", rnd.nextInt(1_000_000)), salt = Long.toHexString(rnd.nextLong());
        try { sms.send("+84" + phone.substring(1), "Ma dang nhap ARES Toys: " + code + ". Hieu luc 5 phut. Khong chia se ma nay cho bat ky ai."); }
        catch (Exception e) { throw new ApiException(HttpStatus.BAD_GATEWAY, "Không gửi được SMS, thử lại sau"); }
        live.put(phone, new Otp(sha(salt + code), salt, Instant.now().plusSeconds(300), new int[]{0}));
        mark(sentByPhone, phone); mark(sentByIp, ip);
        return Map.of("sent", true, "phone", phone.substring(0, 3) + "****" + phone.substring(7), "expiresIn", 300, "resendIn", 60);
    }

    @Transactional
    public User verify(String rawPhone, String code) {
        String phone = normalize(rawPhone); Otp o = phone == null ? null : live.get(phone);
        if (o == null || o.exp().isBefore(Instant.now())) { if (phone != null) live.remove(phone); throw ApiException.bad("Mã đã hết hạn, vui lòng gửi lại"); }
        if (code == null || !MessageDigest.isEqual(sha(o.salt() + code.trim()).getBytes(), o.hash().getBytes())) {
            if (++o.attempts()[0] >= 5) { live.remove(phone); throw ApiException.bad("Sai mã quá 5 lần, vui lòng gửi mã mới"); }
            throw ApiException.bad("Mã không đúng (còn " + (5 - o.attempts()[0]) + " lần)");
        }
        live.remove(phone);
        String em = "phone_" + phone + "@phone.ares.local";
        return users.findFirstByPhone(phone).or(() -> users.findByEmailIgnoreCase(em)).orElseGet(() -> {
            User n = new User(); n.email = em; n.phone = phone; n.fullName = "Khách " + phone.substring(0, 3) + "****" + phone.substring(7); n.provider = "PHONE"; return users.save(n); });
    }
}
