package com.ares.shop.payment;

import com.ares.shop.model.CustomerOrder;
import com.ares.shop.model.Enums;
import com.ares.shop.repo.OrderRepo;
import com.ares.shop.repo.PaymentTransactionRepo;
import com.ares.shop.service.VnPayService;
import com.ares.shop.service.payment.CassoWebhook;
import com.ares.shop.service.payment.Hmac;
import com.ares.shop.service.payment.PayOsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Webhook/IPN đầu-cuối: xác thực, đối soát mã đơn + số tiền, chống xử lý trùng. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentWebhookIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired OrderRepo orders;
    @Autowired PaymentTransactionRepo txs;
    @Autowired CassoWebhook casso;
    final ObjectMapper json = new ObjectMapper();
    static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 1_000_000_000L);

    CustomerOrder newOrder(long total, Enums.PaymentMethod m) {
        CustomerOrder o = new CustomerOrder();
        o.code = "TS99" + String.format("%012d", SEQ.incrementAndGet() % 1_000_000_000_000L); // không trùng mã seed (TS + yyMMdd...)
        o.total = total; o.subtotal = total; o.paymentMethod = m; o.buyerName = "Test";
        return orders.save(o);
    }

    CustomerOrder reload(CustomerOrder o) { return orders.findByCode(o.code).orElseThrow(); }

    MockHttpServletRequestBuilder sepay(String body, String key) {
        var b = post("/api/payment/webhook/sepay").contentType(MediaType.APPLICATION_JSON).content(body);
        return key == null ? b : b.header("Authorization", "Apikey " + key);
    }

    String sepayBody(long id, String content, long amount, String type) {
        return "{\"id\":" + id + ",\"gateway\":\"VietinBank\",\"transactionDate\":\"2026-11-10 10:00:00\",\"accountNumber\":\"108800001234\","
                + "\"code\":null,\"content\":\"" + content + "\",\"transferType\":\"" + type + "\",\"transferAmount\":" + amount
                + ",\"accumulated\":0,\"subAccount\":null,\"referenceCode\":\"FT1\",\"description\":\"\"}";
    }

    @Test void sepayPaysOnceAndIsIdempotent() throws Exception {
        CustomerOrder o = newOrder(150_000, Enums.PaymentMethod.BANK_QR);
        long id = SEQ.incrementAndGet();
        String body = sepayBody(id, "NGUYEN VAN A CK " + o.code, 150_000, "in");

        mvc.perform(sepay(body, "wrong")).andExpect(status().isUnauthorized());
        mvc.perform(sepay(body, null)).andExpect(status().isUnauthorized());
        assertEquals(Enums.PaymentStatus.UNPAID, reload(o).paymentStatus);

        mvc.perform(sepay(body, "test-sepay-key")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.results[0].result").value("APPLIED"));
        CustomerOrder paid = reload(o);
        assertEquals(Enums.PaymentStatus.PAID, paid.paymentStatus);
        assertEquals(150_000, paid.paidAmount);
        assertEquals(Enums.OrderStatus.CONFIRMED, paid.status);

        // Gửi lại y hệt => không xử lý lần 2
        mvc.perform(sepay(body, "test-sepay-key")).andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].result").value("DUPLICATE"));
        assertEquals(1, txs.findByOrderCodeOrderByCreatedAtDesc(o.code).size());
        assertEquals(150_000, reload(o).paidAmount);
    }

    @Test void sepayIgnoresOutgoingUnderpaidAndOtherAccount() throws Exception {
        CustomerOrder o = newOrder(200_000, Enums.PaymentMethod.BANK_QR);
        mvc.perform(sepay(sepayBody(SEQ.incrementAndGet(), o.code, 200_000, "out"), "test-sepay-key"))
                .andExpect(jsonPath("$.results[0].result").value("IGNORED"));
        mvc.perform(sepay(sepayBody(SEQ.incrementAndGet(), o.code, 199_999, "in"), "test-sepay-key"))
                .andExpect(jsonPath("$.results[0].result").value("UNDERPAID"));
        mvc.perform(sepay(sepayBody(SEQ.incrementAndGet(), o.code, 200_000, "in").replace("108800001234", "999"), "test-sepay-key"))
                .andExpect(jsonPath("$.results[0].result").value("OTHER_ACCOUNT"));
        mvc.perform(sepay(sepayBody(SEQ.incrementAndGet(), "khong co ma don", 200_000, "in"), "test-sepay-key"))
                .andExpect(jsonPath("$.results[0].result").value("NO_ORDER"));
        assertEquals(Enums.PaymentStatus.UNPAID, reload(o).paymentStatus);
        // Chuyển dư vẫn được chấp nhận (>= tổng)
        mvc.perform(sepay(sepayBody(SEQ.incrementAndGet(), o.code, 250_000, "in"), "test-sepay-key"))
                .andExpect(jsonPath("$.results[0].result").value("APPLIED"));
        assertEquals(Enums.PaymentStatus.PAID, reload(o).paymentStatus);
    }

    @Test void payosWebhookSignedAndIdempotent() throws Exception {
        CustomerOrder o = newOrder(90_000, Enums.PaymentMethod.BANK_QR);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderCode", 1234567); data.put("amount", 90_000); data.put("description", "CSAB12 " + o.code);
        data.put("accountNumber", "123"); data.put("reference", "REF" + SEQ.incrementAndGet()); data.put("transactionDateTime", "2026-11-10 10:00:00");
        data.put("currency", "VND"); data.put("paymentLinkId", "abc"); data.put("code", "00"); data.put("desc", "success");
        data.put("counterAccountBankId", null); data.put("counterAccountName", "");
        String sig = PayOsService.signData(json.valueToTree(data), "1a54716c8f0efb2744fb28b6e38b25da7f67a925d98bc1c18bd8faaecadd7675");
        Map<String, Object> body = Map.of("code", "00", "desc", "success", "success", true, "data", data, "signature", sig);
        String raw = json.writeValueAsString(body);

        mvc.perform(post("/api/payment/webhook/payos").contentType(MediaType.APPLICATION_JSON).content(raw.replace(sig, "00" + sig.substring(2))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/payment/webhook/payos").contentType(MediaType.APPLICATION_JSON).content(raw))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].result").value("APPLIED"));
        mvc.perform(post("/api/payment/webhook/payos").contentType(MediaType.APPLICATION_JSON).content(raw))
                .andExpect(jsonPath("$.results[0].result").value("DUPLICATE"));
        assertEquals(Enums.PaymentStatus.PAID, reload(o).paymentStatus);
    }

    @Test void cassoV2AndV1() throws Exception {
        CustomerOrder o = newOrder(75_000, Enums.PaymentMethod.BANK_QR);
        String raw = "{\"error\":0,\"data\":{\"id\":" + SEQ.incrementAndGet() + ",\"reference\":\"FT9\",\"description\":\"CK " + o.code + "\","
                + "\"amount\":75000,\"runningBalance\":1,\"transactionDateTime\":\"2026-11-10 10:00:00\",\"accountNumber\":\"108800001234\","
                + "\"bankName\":\"VietinBank\",\"bankAbbreviation\":\"ICB\",\"virtualAccountNumber\":\"\",\"virtualAccountName\":\"\","
                + "\"counterAccountName\":\"\",\"counterAccountNumber\":\"\",\"counterAccountBankId\":\"\",\"counterAccountBankName\":\"\"}}";
        long t = System.currentTimeMillis();
        String sig = casso.signV2(t, raw, "g3oZ950pJQ4k6REhOPGkx37RsXgWz9QJ9RCAZ7i0yagLF32XQZtemQ6r3JIo4MCr");
        mvc.perform(post("/api/payment/webhook/casso").contentType(MediaType.APPLICATION_JSON).content(raw)
                        .header("X-Casso-Signature", "t=" + t + ",v1=" + "0".repeat(128))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/payment/webhook/casso").contentType(MediaType.APPLICATION_JSON).content(raw)
                        .header("X-Casso-Signature", "t=" + t + ",v1=" + sig))
                .andExpect(status().isOk()).andExpect(jsonPath("$.error").value(0)).andExpect(jsonPath("$.results[0].result").value("APPLIED"));
        assertEquals(Enums.PaymentStatus.PAID, reload(o).paymentStatus);

        CustomerOrder o2 = newOrder(10_000, Enums.PaymentMethod.BANK_QR);
        String v1 = "{\"error\":0,\"data\":[{\"id\":" + SEQ.incrementAndGet() + ",\"tid\":\"X1\",\"description\":\"" + o2.code + "\",\"amount\":10000}]}";
        mvc.perform(post("/api/payment/webhook/casso").contentType(MediaType.APPLICATION_JSON).content(v1).header("secure-token", "bad"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/payment/webhook/casso").contentType(MediaType.APPLICATION_JSON).content(v1).header("secure-token", "test-casso-token"))
                .andExpect(jsonPath("$.results[0].result").value("APPLIED"));
        mvc.perform(post("/api/payment/webhook/casso").contentType(MediaType.APPLICATION_JSON).content(v1).header("secure-token", "test-casso-token"))
                .andExpect(jsonPath("$.results[0].result").value("DUPLICATE"));
    }

    Map<String, String> vnpIpn(CustomerOrder o, long amount, String txNo, String resp) {
        Map<String, String> p = new HashMap<>();
        p.put("vnp_Amount", String.valueOf(amount * 100)); p.put("vnp_BankCode", "NCB"); p.put("vnp_OrderInfo", "Thanh toan don hang " + o.code);
        p.put("vnp_ResponseCode", resp); p.put("vnp_TransactionStatus", resp); p.put("vnp_TmnCode", "TESTTMN1");
        p.put("vnp_TransactionNo", txNo); p.put("vnp_TxnRef", o.code); p.put("vnp_PayDate", "20261110120000");
        p.put("vnp_SecureHash", VnPayService.sign(p, "TESTVNPAYSECRET"));
        return p;
    }

    MockHttpServletRequestBuilder get(String url, Map<String, String> p) {
        var b = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url);
        p.forEach(b::param);
        return b;
    }

    @Test void vnpayIpnRules() throws Exception {
        CustomerOrder o = newOrder(150_000, Enums.PaymentMethod.VNPAY);
        String tx = String.valueOf(SEQ.incrementAndGet());
        Map<String, String> bad = vnpIpn(o, 150_000, tx, "00"); bad.put("vnp_SecureHash", "00" + bad.get("vnp_SecureHash").substring(2));
        mvc.perform(get("/api/payment/vnpay/ipn", bad)).andExpect(jsonPath("$.RspCode").value("97"));
        mvc.perform(get("/api/payment/vnpay/ipn", vnpIpn(o, 140_000, tx, "00"))).andExpect(jsonPath("$.RspCode").value("04"));
        // Return URL không bao giờ xác nhận tiền
        mvc.perform(get("/api/payment/vnpay/return", vnpIpn(o, 150_000, tx, "00"))).andExpect(status().isFound());
        assertEquals(Enums.PaymentStatus.UNPAID, reload(o).paymentStatus);
        // Giao dịch thất bại: ghi nhận nhưng không PAID
        mvc.perform(get("/api/payment/vnpay/ipn", vnpIpn(o, 150_000, tx, "24"))).andExpect(jsonPath("$.RspCode").value("00"));
        assertEquals(Enums.PaymentStatus.UNPAID, reload(o).paymentStatus);
        mvc.perform(get("/api/payment/vnpay/ipn", vnpIpn(o, 150_000, tx, "00"))).andExpect(jsonPath("$.RspCode").value("00"));
        assertEquals(Enums.PaymentStatus.PAID, reload(o).paymentStatus);
        mvc.perform(get("/api/payment/vnpay/ipn", vnpIpn(o, 150_000, tx, "00"))).andExpect(jsonPath("$.RspCode").value("02"));
        CustomerOrder ghost = new CustomerOrder(); ghost.code = "TS99999999999999";
        mvc.perform(get("/api/payment/vnpay/ipn", vnpIpn(ghost, 1, "1", "00"))).andExpect(jsonPath("$.RspCode").value("01"));
    }

    String momoIpn(CustomerOrder o, long amount, long transId, int resultCode) throws Exception {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("partnerCode", "MOMOTEST"); p.put("orderId", o.code + "_1731200000000"); p.put("requestId", "r1"); p.put("amount", amount);
        p.put("orderInfo", "x"); p.put("orderType", "momo_wallet"); p.put("transId", transId); p.put("resultCode", resultCode);
        p.put("message", "ok"); p.put("payType", "qr"); p.put("responseTime", 1L); p.put("extraData", "");
        p.put("signature", Hmac.sha256Hex("test-momo-secret", com.ares.shop.service.MoMoService.ipnRawSignature("test-access", p)));
        return json.writeValueAsString(p);
    }

    @Test void momoIpnRules() throws Exception {
        CustomerOrder o = newOrder(120_000, Enums.PaymentMethod.MOMO);
        long trans = SEQ.incrementAndGet();
        String good = momoIpn(o, 120_000, trans, 0);
        mvc.perform(post("/api/payment/momo/ipn").contentType(MediaType.APPLICATION_JSON).content(good.replace("\"amount\":120000", "\"amount\":1")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/payment/momo/ipn").contentType(MediaType.APPLICATION_JSON).content(momoIpn(o, 120_000, trans + 1, 1006)))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/payment/momo/ipn").contentType(MediaType.APPLICATION_JSON).content(momoIpn(o, 100_000, trans + 2, 0)))
                .andExpect(status().isNoContent());
        assertEquals(Enums.PaymentStatus.UNPAID, reload(o).paymentStatus, "thất bại / sai số tiền => chưa PAID");
        mvc.perform(post("/api/payment/momo/ipn").contentType(MediaType.APPLICATION_JSON).content(good)).andExpect(status().isNoContent());
        assertEquals(Enums.PaymentStatus.PAID, reload(o).paymentStatus);
        mvc.perform(post("/api/payment/momo/ipn").contentType(MediaType.APPLICATION_JSON).content(good)).andExpect(status().isNoContent());
        assertEquals(1, txs.findByOrderCodeOrderByCreatedAtDesc(o.code).stream().filter(t -> "APPLIED".equals(t.result)).count());
    }

    @Test void fakeConfirmationEndpointsAreGone() throws Exception {
        CustomerOrder o = newOrder(50_000, Enums.PaymentMethod.BANK_QR);
        String body = "{\"orderCode\":\"" + o.code + "\",\"paymentMethod\":\"BANK_QR\"}";
        mvc.perform(post("/api/payment/sandbox/confirm").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(r -> assertTrue(r.getResponse().getStatus() >= 400));
        mvc.perform(post("/api/payment/simulate-transfer").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(r -> assertTrue(r.getResponse().getStatus() >= 400));
        mvc.perform(post("/api/payment/webhook/bank-transfer").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"" + o.code + "\"}"))
                .andExpect(r -> assertTrue(r.getResponse().getStatus() >= 400));
        mvc.perform(post("/api/payment/dev/simulate-transfer").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(r -> assertTrue(r.getResponse().getStatus() >= 400));
        assertEquals(Enums.PaymentStatus.UNPAID, reload(o).paymentStatus);
    }
}
