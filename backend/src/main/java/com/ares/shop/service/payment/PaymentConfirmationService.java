package com.ares.shop.service.payment;

import com.ares.shop.model.CustomerOrder;
import com.ares.shop.model.Enums;
import com.ares.shop.model.PaymentTransaction;
import com.ares.shop.repo.OrderRepo;
import com.ares.shop.repo.PaymentTransactionRepo;
import com.ares.shop.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Logic dùng chung để xác nhận tiền vào cho mọi nhà cung cấp (SePay, payOS, Casso, MoMo IPN, VNPay IPN).
 * Chỉ được gọi SAU KHI đã xác thực chữ ký/khoá webhook.
 * - Idempotent: mỗi (provider, externalId) chỉ được xử lý 1 lần (ràng buộc UNIQUE trong DB).
 * - Chỉ đánh dấu PAID khi số tiền nhận >= số tiền còn phải trả.
 */
@Service
public class PaymentConfirmationService {
    private static final Logger log = LoggerFactory.getLogger(PaymentConfirmationService.class);
    /** Mã đơn: TS/AU + 14 chữ số (xem OrderService), POS + 12 chữ số (PosController). Ngân hàng có thể chèn khoảng trắng/ký tự khác quanh mã. */
    public static final Pattern ORDER_CODE = Pattern.compile("(TS|AU)\\d{14}|POS\\d{12}");

    public enum Result { APPLIED, DUPLICATE, UNDERPAID, NO_ORDER, ALREADY_PAID, ORDER_CANCELLED, AMOUNT_MISMATCH, IGNORED }

    private final OrderRepo orders; private final OrderService orderService; private final PaymentTransactionRepo txs;

    public PaymentConfirmationService(OrderRepo orders, OrderService orderService, PaymentTransactionRepo txs) {
        this.orders = orders; this.orderService = orderService; this.txs = txs;
    }

    /** Tìm mã đơn trong nội dung chuyển khoản (đã bỏ dấu cách/ký tự đặc biệt, viết hoa). */
    public static List<String> extractOrderCodes(String content) {
        List<String> out = new ArrayList<>();
        if (content == null) return out;
        String norm = content.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        Matcher m = ORDER_CODE.matcher(norm);
        while (m.find()) out.add(m.group());
        // Thử thêm trên chuỗi gốc (trường hợp mã đứng cạnh số khác bị dính khi bỏ ký tự)
        Matcher m2 = ORDER_CODE.matcher(content.toUpperCase(Locale.ROOT));
        while (m2.find()) if (!out.contains(m2.group())) out.add(m2.group());
        return out;
    }

    /** Số tiền còn phải trả ở lần thanh toán này (trả góp: kỳ hiện tại; còn lại: toàn bộ phần chưa trả). */
    public static long amountDue(CustomerOrder o) {
        long remain = Math.max(0, o.total - o.paidAmount);
        if (o.paymentMethod == Enums.PaymentMethod.INSTALLMENT && o.monthlyInstallment > 0)
            return Math.min(remain, o.monthlyInstallment);
        return remain;
    }

    /** Chuyển khoản ngân hàng: tìm mã đơn trong nội dung, chấp nhận số tiền >= số phải trả. */
    @Transactional
    public Result applyBankTransfer(String provider, String externalId, String content, long amount, String raw) {
        String code = null;
        for (String c : extractOrderCodes(content)) { if (orders.findByCode(c).isPresent()) { code = c; break; } }
        return apply(provider, externalId, code, content, amount, raw, false, Enums.PaymentMethod.BANK_QR);
    }

    /** Cổng (MoMo/VNPay): mã đơn đã biết, số tiền phải khớp đúng số phải trả. */
    @Transactional
    public Result applyGateway(String provider, String externalId, String orderCode, long amount, String raw, Enums.PaymentMethod method) {
        return apply(provider, externalId, orderCode, null, amount, raw, true, method);
    }

    /** Kiểm tra trước (không ghi DB) – VNPay cần trả mã 01/02/04 trước khi xử lý. */
    public Optional<CustomerOrder> find(String code) { return code == null ? Optional.empty() : orders.findByCode(code.trim().toUpperCase(Locale.ROOT)); }

    private Result apply(String provider, String externalId, String code, String content, long amount, String raw,
                         boolean exactAmount, Enums.PaymentMethod method) {
        if (externalId == null || externalId.isBlank()) throw new IllegalArgumentException("missing transaction id");
        if (txs.findByProviderAndExternalId(provider, externalId).isPresent()) return Result.DUPLICATE;

        PaymentTransaction tx = new PaymentTransaction();
        tx.provider = provider; tx.externalId = externalId; tx.amount = amount; tx.orderCode = code;
        tx.content = content == null ? null : content.substring(0, Math.min(500, content.length()));
        tx.rawPayload = raw;

        Result r;
        CustomerOrder o = code == null ? null : orders.lockByCode(code.toUpperCase(Locale.ROOT)).orElse(null);
        if (o == null) r = Result.NO_ORDER;
        else if (o.status == Enums.OrderStatus.CANCELLED) r = Result.ORDER_CANCELLED;
        else if (o.paymentStatus == Enums.PaymentStatus.PAID) r = Result.ALREADY_PAID;
        else {
            long due = amountDue(o);
            if (exactAmount && amount != due) r = Result.AMOUNT_MISMATCH;
            else if (!exactAmount && amount < due) r = Result.UNDERPAID;
            else {
                if (o.paymentMethod != Enums.PaymentMethod.INSTALLMENT) o.paymentMethod = method;
                orderService.pay(o);
                o.transactionRef = provider + "-" + externalId;
                if (o.status == Enums.OrderStatus.PENDING) {
                    o.status = Enums.OrderStatus.CONFIRMED;
                    o.timeline.putIfAbsent("CONFIRMED", java.time.LocalDateTime.now());
                }
                orders.save(o);
                r = Result.APPLIED;
            }
        }
        tx.result = r.name();
        try {
            txs.saveAndFlush(tx);
        } catch (DataIntegrityViolationException dup) {
            // Một webhook trùng chạy song song đã ghi trước => huỷ toàn bộ thay đổi của lần này
            throw new DuplicateTransactionException();
        }
        log.info("Payment {} tx={} order={} amount={} => {}", provider, externalId, code, amount, r);
        return r;
    }

    /** Ném ra để rollback transaction khi gặp bản ghi trùng do chạy song song. */
    public static class DuplicateTransactionException extends RuntimeException {
        public DuplicateTransactionException() { super("duplicate transaction"); }
    }
}
