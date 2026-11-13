package com.ares.shop.payment;

import com.ares.shop.service.MoMoService;
import com.ares.shop.service.VnPayService;
import com.ares.shop.service.payment.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Kiểm tra chữ ký của từng nhà cung cấp, dùng vector mẫu chính thức (payOS, Casso) và vector tính độc lập (VNPay, MoMo). */
class SignatureVerificationTest {
    static PaymentProps props() {
        PaymentProps p = new PaymentProps();
        p.frontendUrl = "http://localhost:5173";
        p.sepayEnabled = true; p.sepayApiKey = "sepay-secret-key";
        p.payosEnabled = true; p.payosClientId = "c"; p.payosApiKey = "a";
        p.payosChecksumKey = "1a54716c8f0efb2744fb28b6e38b25da7f67a925d98bc1c18bd8faaecadd7675";
        p.cassoEnabled = true; p.cassoChecksumKey = CASSO_KEY;
        p.cassoSecureToken = "casso-v1-token"; p.cassoMaxAgeSeconds = 0;
        p.momoEnabled = true; p.momoPartnerCode = "MOMOTEST"; p.momoAccessKey = "test-access"; p.momoSecretKey = "test-momo-secret";
        p.vnpayEnabled = true; p.vnpayTmnCode = "TESTTMN1"; p.vnpayHashSecret = "TESTVNPAYSECRET";
        return p;
    }

    // ---------- HMAC (RFC 4231 test case 2)
    @Test void hmacMatchesRfc4231() {
        assertEquals("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843", Hmac.sha256Hex("Jefe", "what do ya want for nothing?"));
        assertEquals("164b7a7bfcf819e2e395fbe73b56e0a387bd64222e831fd610270cd7ea2505549758bf75c05a994a6d034f65f8f0e6fdcaeab1a34d4a6b4b636e070a38bce737",
                Hmac.sha512Hex("Jefe", "what do ya want for nothing?"));
    }

    // ---------- SePay
    @Test void sepayApiKey() {
        SePayWebhook s = new SePayWebhook(props());
        assertTrue(s.verify(Map.of("authorization", "Apikey sepay-secret-key"), "{}"));
        assertTrue(s.verify(Map.of("authorization", "apikey sepay-secret-key"), "{}"));
        assertFalse(s.verify(Map.of("authorization", "Apikey wrong"), "{}"));
        assertFalse(s.verify(Map.of("authorization", "Bearer sepay-secret-key"), "{}"));
        assertFalse(s.verify(Map.of(), "{}"));
    }

    @Test void sepayParse() throws Exception {
        String body = "{\"id\":92704,\"gateway\":\"VietinBank\",\"transactionDate\":\"2026-11-10 10:00:00\",\"accountNumber\":\"108800001234\",\"code\":null,"
                + "\"content\":\"NGUYEN VAN A chuyen tien TS26111000000001\",\"transferType\":\"in\",\"transferAmount\":150000,\"accumulated\":0,"
                + "\"subAccount\":null,\"referenceCode\":\"MBVCB.1\",\"description\":\"\"}";
        BankTransfer t = new SePayWebhook(props()).parse(body).get(0);
        assertEquals("92704", t.externalId()); assertEquals(150000, t.amount()); assertTrue(t.incoming());
        assertEquals(List.of("TS26111000000001"), PaymentConfirmationService.extractOrderCodes(t.content()));
    }

    // ---------- payOS (dữ liệu mẫu chính thức trong tài liệu payOS)
    static final String PAYOS_SAMPLE = "{\"code\":\"00\",\"desc\":\"success\",\"success\":true,\"data\":{\"orderCode\":123,\"amount\":3000,"
            + "\"description\":\"VQRIO123\",\"accountNumber\":\"12345678\",\"reference\":\"TF230204212323\",\"transactionDateTime\":\"2023-02-04 18:25:00\","
            + "\"currency\":\"VND\",\"paymentLinkId\":\"124c33293c43417ab7879e14c8d9eb18\",\"code\":\"00\",\"desc\":\"Thành công\",\"counterAccountBankId\":\"\","
            + "\"counterAccountBankName\":\"\",\"counterAccountName\":\"\",\"counterAccountNumber\":\"\",\"virtualAccountName\":\"\",\"virtualAccountNumber\":\"\"},"
            + "\"signature\":\"412e915d2871504ed31be63c8f62a149a4410d34c4c42affc9006ef9917eaa03\"}";

    @Test void payosOfficialSampleValid() {
        assertTrue(new PayOsService(props()).verify(Map.of(), PAYOS_SAMPLE));
    }

    @Test void payosTamperedInvalid() {
        PayOsService s = new PayOsService(props());
        assertFalse(s.verify(Map.of(), PAYOS_SAMPLE.replace("\"amount\":3000", "\"amount\":3000000")));
        assertFalse(s.verify(Map.of(), PAYOS_SAMPLE.replace("412e915d", "000e915d")));
        PaymentProps other = props(); other.payosChecksumKey = "wrong";
        assertFalse(new PayOsService(other).verify(Map.of(), PAYOS_SAMPLE));
    }

    // ---------- Casso V2 (dữ liệu mẫu chính thức: curl mẫu trong developer.casso.vn + java.java của CassoHQ)
    static final String CASSO_KEY = "mgExMrjj4i7a1p8Cs6LmFbpCd8lbw4sSX4LZXPag7TKazeIogtR90tvWYTkwQbvs";
    static final String CASSO_SAMPLE = "{\"error\":0,\"data\":{\"id\":0,\"reference\":\"MA_GIAO_DICH_THU_NGHIEM\",\"description\":\"giao dich thu nghiem\","
            + "\"amount\":599000,\"runningBalance\":25000000,\"transactionDateTime\":\"2024-10-03 15:06:37\",\"accountNumber\":\"88888888\","
            + "\"bankName\":\"VPBank\",\"bankAbbreviation\":\"VPB\",\"virtualAccountNumber\":\"\",\"virtualAccountName\":\"\",\"counterAccountName\":\"NGUYEN VAN A\","
            + "\"counterAccountNumber\":\"8888888888\",\"counterAccountBankId\":\"970415\",\"counterAccountBankName\":\"VietinBank\"}}";
    static final String CASSO_SIG = "t=1727948258788,v1=ed0a4bd2e826d5cb69988cdb141e6c1a080e21f3b57eb72cd78192220042b9e7dde0868fc667faea8e224900fa7904e7c88dfa098032fb2d6b6996856e8b7ff3";

    @Test void cassoV2OfficialSampleValid() {
        assertTrue(new CassoWebhook(props()).verify(Map.of("x-casso-signature", CASSO_SIG), CASSO_SAMPLE));
    }

    @Test void cassoV2TamperedOrExpiredInvalid() {
        CassoWebhook c = new CassoWebhook(props());
        assertFalse(c.verify(Map.of("x-casso-signature", CASSO_SIG), CASSO_SAMPLE.replace("599000,", "999000,")));
        assertFalse(c.verify(Map.of("x-casso-signature", CASSO_SIG.replace("t=1727948258788", "t=1727948258789")), CASSO_SAMPLE));
        assertFalse(c.verify(Map.of("x-casso-signature", "garbage"), CASSO_SAMPLE));
        PaymentProps p = props(); p.cassoMaxAgeSeconds = 300; // chữ ký mẫu từ 10/2024 => quá hạn
        assertFalse(new CassoWebhook(p).verify(Map.of("x-casso-signature", CASSO_SIG), CASSO_SAMPLE));
    }

    @Test void cassoV1SecureToken() {
        CassoWebhook c = new CassoWebhook(props());
        assertTrue(c.verify(Map.of("secure-token", "casso-v1-token"), "{}"));
        assertFalse(c.verify(Map.of("secure-token", "nope"), "{}"));
        assertFalse(c.verify(Map.of(), "{}"));
    }

    @Test void cassoParseV1AndV2() throws Exception {
        CassoWebhook c = new CassoWebhook(props());
        BankTransfer v2 = c.parse(CASSO_SAMPLE).get(0);
        assertEquals("MA_GIAO_DICH_THU_NGHIEM", v2.externalId(), "id=0 (giao dịch thử) => dùng reference"); assertEquals(599000, v2.amount()); assertTrue(v2.incoming());
        List<BankTransfer> v1 = c.parse("{\"error\":0,\"data\":[{\"id\":1,\"tid\":\"A\",\"description\":\"x\",\"amount\":-5000},{\"id\":2,\"tid\":\"B\",\"description\":\"y\",\"amount\":7000}]}");
        assertEquals(2, v1.size()); assertFalse(v1.get(0).incoming()); assertTrue(v1.get(1).incoming());
    }

    // ---------- VNPay (vector tính độc lập bằng Python hmac/urllib)
    static Map<String, String> vnpParams() {
        Map<String, String> p = new HashMap<>();
        p.put("vnp_Amount", "15000000"); p.put("vnp_BankCode", "NCB");
        p.put("vnp_OrderInfo", "Thanh toan don hang TS26111000000001"); p.put("vnp_ResponseCode", "00");
        p.put("vnp_TmnCode", "TESTTMN1"); p.put("vnp_TransactionNo", "14012345"); p.put("vnp_TransactionStatus", "00");
        p.put("vnp_TxnRef", "TS26111000000001"); p.put("vnp_PayDate", "20261110120000");
        return p;
    }
    static final String VNP_EXPECTED = "7feec099b59502764fee28dbdf8d65449a2bdd8f570a38d00ef7ecdcd9a176f279ad5f8ab98d2cef34024e56b20b5ef739af2fb5abd98e897aba37161adf7525";

    @Test void vnpayKnownVector() {
        assertEquals(VNP_EXPECTED, VnPayService.sign(vnpParams(), "TESTVNPAYSECRET"));
        Map<String, String> p = vnpParams(); p.put("vnp_SecureHash", VNP_EXPECTED.toUpperCase()); p.put("vnp_SecureHashType", "HmacSHA512");
        assertTrue(new VnPayService(props()).verify(p));
    }

    @Test void vnpayTamperedInvalid() {
        VnPayService v = new VnPayService(props());
        Map<String, String> p = vnpParams(); p.put("vnp_SecureHash", VNP_EXPECTED); p.put("vnp_Amount", "15000100");
        assertFalse(v.verify(p));
        Map<String, String> q = vnpParams(); q.put("vnp_TmnCode", "OTHER"); q.put("vnp_SecureHash", VnPayService.sign(q, "TESTVNPAYSECRET"));
        assertFalse(v.verify(q), "khác merchant");
        Map<String, String> r = vnpParams();
        assertFalse(v.verify(r), "thiếu chữ ký");
    }

    // ---------- MoMo (vector tính độc lập bằng Python hmac)
    static Map<String, Object> momoIpn() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("partnerCode", "MOMOTEST"); p.put("orderId", "TS26111000000001_1731200000000"); p.put("requestId", "req-1");
        p.put("amount", 150000); p.put("orderInfo", "Thanh toan don hang TS26111000000001 tai ARES Toys"); p.put("orderType", "momo_wallet");
        p.put("transId", 4088878653L); p.put("resultCode", 0); p.put("message", "Successful."); p.put("payType", "qr");
        p.put("responseTime", 1731200001000L); p.put("extraData", "");
        p.put("signature", "74cf3b2dd13f97e05fd35ebda36428b5d3049c71b503fd658d715bc47c4ff29a");
        return p;
    }

    @Test void momoKnownVectorValid() {
        assertTrue(new MoMoService(props()).verifyIpn(momoIpn()));
        assertEquals("TS26111000000001", MoMoService.orderCodeOf("TS26111000000001_1731200000000"));
    }

    @Test void momoTamperedInvalid() {
        MoMoService m = new MoMoService(props());
        Map<String, Object> p = momoIpn(); p.put("amount", 1000);
        assertFalse(m.verifyIpn(p));
        Map<String, Object> q = momoIpn(); q.put("partnerCode", "OTHER");
        assertFalse(m.verifyIpn(q));
        PaymentProps off = props(); off.momoEnabled = false;
        assertFalse(new MoMoService(off).verifyIpn(momoIpn()), "tắt MoMo => không chấp nhận");
    }

    @Test void orderCodeExtraction() {
        assertEquals(List.of("TS26111000000001"), PaymentConfirmationService.extractOrderCodes("ck don ts 2611 1000 0000 01 cam on"));
        assertEquals(List.of("POS261110120000"), PaymentConfirmationService.extractOrderCodes("POS261110120000"));
        assertTrue(PaymentConfirmationService.extractOrderCodes("chuyen tien an trua").isEmpty());
    }

    @Test void vietQrUsesConfiguredAccount() {
        PaymentProps p = props(); p.bankBin = "970415"; p.bankAccountNo = "108800001234"; p.bankAccountName = "ARES TOYS"; p.qrTemplate = "compact2";
        assertEquals("https://img.vietqr.io/image/970415-108800001234-compact2.png?amount=150000&addInfo=TS26111000000001&accountName=ARES%20TOYS",
                p.vietQrUrl("TS26111000000001", 150000));
        PaymentProps none = props(); none.bankBin = "970415"; none.bankAccountNo = "";
        assertEquals("", none.vietQrUrl("X", 1));
        assertNotNull(new ObjectMapper());
    }
}
