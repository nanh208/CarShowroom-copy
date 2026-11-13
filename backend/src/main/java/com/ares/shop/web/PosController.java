package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.ares.shop.service.OrderService;
import com.ares.shop.service.MoMoService;
import com.ares.shop.service.VnPayService;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/admin/pos")
public class PosController {

    private final ProductRepo products;
    private final VariantRepo variants;
    private final UserRepo users;
    private final OrderRepo orders;
    private final PosShiftRepo shifts;
    private final StaffCommissionTxRepo commissionTxRepo;
    private final StockRepo stockRepo;
    private final PointRepo pointRepo;
    private final CategoryRepo categories;
    private final VnPayService vnPayService;
    private final MoMoService moMoService;
    private final com.ares.shop.service.payment.PaymentProps paymentProps;
    private final OrderService orderService;

    public PosController(ProductRepo products, VariantRepo variants, UserRepo users,
                         OrderRepo orders, PosShiftRepo shifts,
                         StaffCommissionTxRepo commissionTxRepo, StockRepo stockRepo,
                         PointRepo pointRepo, CategoryRepo categories,
                         VnPayService vnPayService, MoMoService moMoService, OrderService orderService,
                         com.ares.shop.service.payment.PaymentProps paymentProps) {
        this.paymentProps = paymentProps;
        this.products = products;
        this.variants = variants;
        this.users = users;
        this.orders = orders;
        this.shifts = shifts;
        this.commissionTxRepo = commissionTxRepo;
        this.stockRepo = stockRepo;
        this.pointRepo = pointRepo;
        this.categories = categories;
        this.vnPayService = vnPayService;
        this.moMoService = moMoService;
        this.orderService = orderService;
    }

    /** Danh mục sản phẩm bán tại quầy POS với thông tin tồn kho, mã vạch, biến thể. */
    @GetMapping("/catalog")
    public List<Map<String, Object>> getCatalog(@RequestParam(required = false) String q,
                                                @RequestParam(required = false) String categoryCode,
                                                @RequestParam(required = false) Long brandId) {
        var list = products.findAll(Sort.by("id").descending());
        String kw = q != null ? q.trim().toLowerCase() : "";

        return list.stream()
                .filter(p -> p.active)
                .filter(p -> categoryCode == null || categoryCode.isBlank() || (p.type != null && p.type.equalsIgnoreCase(categoryCode)))
                .filter(p -> brandId == null || (p.brand != null && brandId.equals(p.brand.id)))
                .filter(p -> {
                    if (kw.isEmpty()) return true;
                    String text = (p.name + " " + (p.sku != null ? p.sku : "") + " " +
                            (p.type != null ? p.type : "") + " " +
                            (p.brand != null ? p.brand.name : "")).toLowerCase();
                    return text.contains(kw);
                })
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", p.id);
                    m.put("name", p.name);
                    m.put("sku", p.sku != null && !p.sku.isBlank() ? p.sku : "SKU-" + p.id);
                    m.put("barcode", p.sku != null && !p.sku.isBlank() ? p.sku : String.format("893000%06d", p.id));
                    m.put("price", p.price);
                    m.put("oldPrice", p.oldPrice);
                    m.put("stock", p.stock);
                    m.put("scale", p.scale);
                    m.put("category", p.type != null ? p.type : "");
                    m.put("brand", p.brand != null ? p.brand.name : "");
                    m.put("image", p.images != null && !p.images.isEmpty() ? p.images.get(0) : "");

                    // Biến thể màu sắc / phiên bản
                    var varList = variants.findByProductIdOrderByIdAsc(p.id);
                    m.put("variants", varList.stream().map(v -> {
                        Map<String, Object> vm = new LinkedHashMap<>();
                        vm.put("id", v.id);
                        vm.put("name", v.name);
                        vm.put("color", v.color);
                        vm.put("price", v.price > 0 ? v.price : p.price);
                        vm.put("stock", v.stock);
                        vm.put("sku", m.get("sku") + "-" + v.id);
                        return vm;
                    }).toList());

                    return m;
                }).toList();
    }

    /** Tra cứu khách hàng theo SĐT, tên hoặc mã thẻ VIP. */
    @GetMapping("/customers")
    public List<Map<String, Object>> searchCustomers(@RequestParam String q) {
        String kw = q.trim().toLowerCase();
        if (kw.length() < 2) return List.of();

        return users.findAll().stream()
                .filter(u -> u.role == Enums.Role.USER)
                .filter(u -> {
                    String s = (u.fullName + " " + (u.phone != null ? u.phone : "") + " " +
                            u.email + " " + (u.vipCardNumber != null ? u.vipCardNumber : "")).toLowerCase();
                    return s.contains(kw);
                })
                .limit(10)
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", u.id);
                    m.put("fullName", u.fullName);
                    m.put("phone", u.phone);
                    m.put("email", u.email);
                    m.put("tier", u.tier != null ? u.tier : "Đồng");
                    m.put("vipCardNumber", u.vipCardNumber);
                    m.put("points", u.points);
                    m.put("storeCredit", u.storeCredit != null ? u.storeCredit : 0L);

                    // Tỷ lệ ưu đãi giảm giá theo hạng VIP
                    int discountPercent = 0;
                    if ("Bạc".equalsIgnoreCase(u.tier)) discountPercent = 2;
                    else if ("Vàng".equalsIgnoreCase(u.tier)) discountPercent = 3;
                    else if ("Titan".equalsIgnoreCase(u.tier) || "Bạch kim".equalsIgnoreCase(u.tier)) discountPercent = 5;
                    m.put("tierDiscountPercent", discountPercent);

                    return m;
                }).toList();
    }

    /** Tạo và hoàn tất đơn bán lẻ tại quầy (POS Order). */
    @PostMapping("/orders")
    @Transactional
    public Map<String, Object> createPosOrder(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemsReq = (List<Map<String, Object>>) body.get("items");
        if (itemsReq == null || itemsReq.isEmpty()) {
            throw ApiException.bad("Giỏ hàng tại quầy trống, vui lòng chọn ít nhất 1 sản phẩm");
        }

        // Thông tin nhân viên bán hàng
        Long staffId = null;
        if (body.get("staffId") != null) {
            staffId = Long.valueOf(body.get("staffId").toString());
        }
        User staffUser = null;
        if (staffId != null) {
            staffUser = users.findById(staffId).orElse(null);
        }
        if (staffUser == null) {
            staffUser = Current.user();
            if (staffUser != null) staffId = staffUser.id;
        }
        String staffName = staffUser != null ? staffUser.fullName : "Thu ngân showroom";

        // Thông tin khách hàng
        Long customerId = null;
        if (body.get("customerId") != null) {
            customerId = Long.valueOf(body.get("customerId").toString());
        }
        User customer = customerId != null ? users.findById(customerId).orElse(null) : null;
        String buyerName = body.get("customerName") != null ? body.get("customerName").toString().trim() : "";
        if (buyerName.isBlank()) {
            buyerName = customer != null ? customer.fullName : "Khách lẻ tại quầy";
        }
        String phone = body.get("customerPhone") != null ? body.get("customerPhone").toString().trim() : "";
        if (phone.isBlank() && customer != null) {
            phone = customer.phone != null ? customer.phone : "";
        }

        // Tạo hóa đơn POS
        CustomerOrder o = new CustomerOrder();
        o.code = "POS" + DateTimeFormatter.ofPattern("yyMMddHHmmss").format(LocalDateTime.now()) + (int)(Math.random() * 900 + 100);
        o.channel = "POS";
        o.user = customer;
        o.buyerName = buyerName;
        o.phone = phone;
        o.email = customer != null ? customer.email : "khachle@quayares.vn";
        o.shippingAddress = "Nhận hàng trực tiếp tại showroom ARES Toys (Hải Phòng)";
        o.city = "Hải Phòng";
        o.staffId = staffId;
        o.staffName = staffName;
        o.guest = (customer == null);
        o.note = body.get("note") != null ? body.get("note").toString() : "Bán lẻ tại quầy";

        // Xử lý ca làm việc hiện tại
        PosShift activeShift = shifts.findFirstByStatusOrderByStartTimeDesc("OPEN").orElse(null);
        if (activeShift != null) {
            o.shiftId = activeShift.id;
        }

        // Thêm chi tiết món & trừ kho
        long subtotal = 0;
        int totalItemQty = 0;
        for (var it : itemsReq) {
            Long pid = Long.valueOf(it.get("productId").toString());
            Long vid = it.get("variantId") != null ? Long.valueOf(it.get("variantId").toString()) : null;
            int qty = it.get("quantity") != null ? Integer.parseInt(it.get("quantity").toString()) : 1;
            if (qty <= 0) qty = 1;

            Product p = products.findById(pid).orElseThrow(() -> ApiException.bad("Không tìm thấy sản phẩm ID " + pid));
            ProductVariant pv = vid != null ? variants.findById(vid).orElse(null) : null;

            long unitPrice = p.price;
            if (pv != null && pv.price > 0) {
                unitPrice = pv.price;
            }
            if (it.get("unitPrice") != null) {
                unitPrice = Long.parseLong(it.get("unitPrice").toString());
            }

            // Trừ tồn kho sản phẩm
            if (p.stock < qty) {
                throw ApiException.bad("Sản phẩm [" + p.name + "] chỉ còn tồn kho " + p.stock + " chiếc");
            }
            p.stock -= qty;
            p.sold += qty;
            products.save(p);

            // Trừ biến thể nếu có
            if (pv != null) {
                if (pv.stock < qty) {
                    throw ApiException.bad("Phiên bản [" + pv.name + "] của " + p.name + " chỉ còn " + pv.stock + " chiếc");
                }
                pv.stock -= qty;
                variants.save(pv);
            }

            // Ghi nhận biến động kho
            StockMovement sm = new StockMovement();
            sm.productId = p.id;
            sm.variantId = pv != null ? pv.id : null;
            sm.productName = p.name + (pv != null ? " (" + pv.name + ")" : "");
            sm.delta = -qty;
            sm.after = p.stock;
            sm.reason = "POS: Bán tại quầy (" + o.code + ")";
            sm.byUser = staffName;
            sm.createdAt = LocalDateTime.now();
            stockRepo.save(sm);

            // Tạo OrderItem
            OrderItem oi = new OrderItem();
            oi.order = o;
            oi.productId = p.id;
            oi.variantId = pv != null ? pv.id : null;
            oi.productName = p.name;
            oi.variantName = pv != null ? pv.name : null;
            oi.image = p.images != null && !p.images.isEmpty() ? p.images.get(0) : "";
            oi.sku = p.sku;
            oi.unitPrice = unitPrice;
            oi.quantity = qty;
            o.items.add(oi);

            subtotal += unitPrice * qty;
            totalItemQty += qty;
        }
        o.subtotal = subtotal;

        // Giảm giá / Chiết khấu thông thường tại quầy (Voucher / Điểm)
        long discount = 0;
        if (body.get("discountAmount") != null) {
            discount = Long.parseLong(body.get("discountAmount").toString());
        }
        o.discount = discount;

        // Thẩm quyền Quản lý: Phê duyệt chiết khấu đặc quyền (Manager Discount Override)
        long managerDiscount = 0;
        if (body.get("managerDiscountAmount") != null) {
            managerDiscount = Long.parseLong(body.get("managerDiscountAmount").toString());
        }
        o.managerDiscountAmount = managerDiscount;
        o.managerDiscountReason = body.get("managerDiscountReason") != null ? body.get("managerDiscountReason").toString() : "";
        User curUser = Current.user();
        if (managerDiscount > 0 && curUser != null && curUser.role == Enums.Role.ADMIN) {
            o.managerApprovedBy = curUser.fullName;
        }

        // Dùng điểm thưởng
        int pointsUsed = 0;
        long pointsDiscount = 0;
        if (body.get("usePoints") != null && customer != null) {
            pointsUsed = Integer.parseInt(body.get("usePoints").toString());
            if (pointsUsed > customer.points) pointsUsed = customer.points;
            if (pointsUsed > 0) {
                pointsDiscount = (long) pointsUsed * 1000L;
                long maxRedeem = Math.max(0, subtotal - discount - managerDiscount);
                if (pointsDiscount > maxRedeem) {
                    pointsDiscount = maxRedeem;
                    pointsUsed = (int)(pointsDiscount / 1000L);
                }
                customer.points -= pointsUsed;
                users.save(customer);

                // Ghi nhận giao dịch điểm
                PointTx ptx = new PointTx();
                ptx.user = customer;
                ptx.delta = -pointsUsed;
                ptx.reason = "Dùng điểm trừ tiền tại quầy hóa đơn " + o.code;
                ptx.createdAt = LocalDateTime.now();
                pointRepo.save(ptx);
            }
        }
        o.pointsUsed = pointsUsed;
        o.pointsDiscount = pointsDiscount;
        o.shippingFee = 0; // Bán tại quầy không tính phí ship

        // Hóa đơn VAT Doanh nghiệp
        boolean requireVat = Boolean.TRUE.equals(body.get("requireVatInvoice"));
        o.requireVatInvoice = requireVat;
        if (requireVat) {
            o.companyName = body.get("companyName") != null ? body.get("companyName").toString().trim() : "";
            o.companyTaxCode = body.get("companyTaxCode") != null ? body.get("companyTaxCode").toString().trim() : "";
            o.companyAddress = body.get("companyAddress") != null ? body.get("companyAddress").toString().trim() : "";
            o.companyEmail = body.get("companyEmail") != null ? body.get("companyEmail").toString().trim() : "";
        }

        long totalBeforeVat = Math.max(0, subtotal - discount - managerDiscount - pointsDiscount);
        long vatAmount = requireVat ? Math.round(totalBeforeVat * 0.08) : 0L; // VAT 8%
        o.vatAmount = vatAmount;

        long total = totalBeforeVat + vatAmount;
        o.total = total;
        o.paidAmount = total;

        // Phương thức thanh toán POS
        String posMethod = body.get("paymentMethod") != null ? body.get("paymentMethod").toString().toUpperCase() : "CASH";
        o.posPaymentMethod = posMethod;

        if ("CASH".equalsIgnoreCase(posMethod)) {
            o.paymentMethod = Enums.PaymentMethod.COD;
            long cashGiven = body.get("cashGiven") != null ? Long.parseLong(body.get("cashGiven").toString()) : total;
            o.cashGiven = cashGiven;
            o.cashChange = Math.max(0, cashGiven - total);
        } else if ("BANK_QR".equalsIgnoreCase(posMethod) || "VIETQR".equalsIgnoreCase(posMethod)) {
            o.paymentMethod = Enums.PaymentMethod.BANK_QR;
            o.cashGiven = total;
            o.cashChange = 0L;
        } else if ("VNPAY".equalsIgnoreCase(posMethod)) {
            o.paymentMethod = Enums.PaymentMethod.VNPAY;
            o.cashGiven = total;
            o.cashChange = 0L;
        } else if ("MOMO".equalsIgnoreCase(posMethod)) {
            o.paymentMethod = Enums.PaymentMethod.MOMO;
            o.cashGiven = total;
            o.cashChange = 0L;
        } else if ("POS_CARD".equalsIgnoreCase(posMethod) || "CARD".equalsIgnoreCase(posMethod)) {
            o.paymentMethod = Enums.PaymentMethod.BANK_QR;
            o.cashGiven = total;
            o.cashChange = 0L;
        } else {
            o.paymentMethod = Enums.PaymentMethod.COD;
            o.cashGiven = total;
            o.cashChange = 0L;
        }

        o.paymentStatus = Enums.PaymentStatus.PAID;
        o.status = Enums.OrderStatus.COMPLETED;
        o.paidAt = LocalDateTime.now();

        // Tích điểm thưởng cho khách hàng nếu có tài khoản: 1 điểm cho mỗi 10.000đ
        if (customer != null && total > 0) {
            int earned = (int)(total / 10000L);
            o.pointsEarned = earned;
            customer.points += earned;
            orderService.updateTier(customer);
            users.save(customer);

            PointTx ptx = new PointTx();
            ptx.user = customer;
            ptx.delta = earned;
            ptx.reason = "Tích điểm mua hàng tại quầy " + o.code;
            ptx.createdAt = LocalDateTime.now();
            pointRepo.save(ptx);
        }

        // Tính thưởng hoa hồng cho nhân viên bán hàng
        double commRate = (staffUser != null && staffUser.commissionRate != null) ? staffUser.commissionRate : 2.0;
        long commissionAmount = Math.round(total * commRate / 100.0);
        o.commissionRate = commRate;
        o.commissionAmount = commissionAmount;
        o.commissionStatus = "PENDING";

        // Timeline
        o.timeline.put(Enums.OrderStatus.PENDING.name(), LocalDateTime.now());
        o.timeline.put(Enums.OrderStatus.COMPLETED.name(), LocalDateTime.now());

        // Lưu đơn hàng
        orders.save(o);

        // Ghi nhận giao dịch hoa hồng nhân viên
        if (staffId != null && commissionAmount > 0) {
            StaffCommissionTx ctx = new StaffCommissionTx();
            ctx.staffId = staffId;
            ctx.staffName = staffName;
            ctx.txType = "EARN";
            ctx.orderId = o.id;
            ctx.orderCode = o.code;
            ctx.amount = commissionAmount;
            ctx.rate = commRate;
            ctx.note = "Thưởng hoa hồng " + commRate + "% từ hóa đơn POS " + o.code + " (" + total + "đ)";
            ctx.createdBy = "POS_SYSTEM";
            ctx.createdAt = LocalDateTime.now();
            commissionTxRepo.save(ctx);
        }

        // Cập nhật thống kê ca làm việc nếu có
        if (activeShift != null) {
            activeShift.totalSales += total;
            activeShift.orderCount += 1;
            activeShift.itemCount += totalItemQty;
            if ("CASH".equalsIgnoreCase(posMethod)) {
                activeShift.cashSales += total;
                activeShift.expectedCash += total;
            } else if ("BANK_QR".equalsIgnoreCase(posMethod)) {
                activeShift.bankQrSales += total;
            } else if ("POS_CARD".equalsIgnoreCase(posMethod) || "CARD".equalsIgnoreCase(posMethod)) {
                activeShift.cardSales += total;
            } else {
                activeShift.otherSales += total;
            }
            shifts.save(activeShift);
        }

        // Chuẩn bị kết quả trả về đầy đủ cho việc in hóa đơn bán lẻ và phục vụ khách hàng
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("orderId", o.id);
        res.put("code", o.code);
        res.put("createdAt", o.createdAt);
        res.put("buyerName", o.buyerName);
        res.put("phone", o.phone);
        res.put("staffName", o.staffName);
        res.put("subtotal", o.subtotal);
        res.put("discount", o.discount);
        res.put("managerDiscountAmount", o.managerDiscountAmount);
        res.put("managerDiscountReason", o.managerDiscountReason);
        res.put("managerApprovedBy", o.managerApprovedBy);
        res.put("pointsDiscount", o.pointsDiscount);
        res.put("vatAmount", o.vatAmount);
        res.put("requireVatInvoice", o.requireVatInvoice);
        res.put("companyName", o.companyName);
        res.put("companyTaxCode", o.companyTaxCode);
        res.put("companyAddress", o.companyAddress);
        res.put("companyEmail", o.companyEmail);
        res.put("total", o.total);
        res.put("paymentMethod", o.paymentMethod.name());
        res.put("posPaymentMethod", o.posPaymentMethod);
        res.put("cashGiven", o.cashGiven);
        res.put("cashChange", o.cashChange);
        res.put("commissionAmount", o.commissionAmount);

        // Sinh liên kết Cổng thanh toán Sandbox chính thức cho màn hình phục vụ khách hàng
        try {
            if (vnPayService.ready()) res.put("vnpayUrl", vnPayService.createPaymentUrl(o, o.total, "127.0.0.1"));
        } catch (Exception ignored) {}
        try {
            var momoRes = moMoService.createPayment(o, o.total);
            res.put("momoPayUrl", momoRes.get("payUrl"));
            res.put("momoQrCodeUrl", momoRes.get("qrCodeUrl"));
        } catch (Exception ignored) {}
        res.put("vietqrUrl", paymentProps.vietQrUrl(o.code, o.total));

        res.put("items", o.items.stream().map(i -> Map.of(
                "productName", i.productName,
                "variantName", i.variantName != null ? i.variantName : "",
                "unitPrice", i.unitPrice,
                "quantity", i.quantity,
                "total", i.unitPrice * i.quantity
        )).toList());

        return res;
    }

    /** Tạo liên kết thanh toán & lưu đơn chờ chuyển tiền vào tài khoản VietinBank / MoMo của chủ shop. */
    @PostMapping("/generate-sandbox-pay")
    @Transactional
    public Map<String, Object> generateSandboxPay(@RequestBody Map<String, Object> body) {
        String method = body.get("method") != null ? body.get("method").toString().toUpperCase() : "VIETQR";
        long amount = body.get("amount") != null ? Long.parseLong(body.get("amount").toString()) : 100000L;
        String tempCode = body.get("code") != null ? body.get("code").toString() : ("POS" + DateTimeFormatter.ofPattern("yyMMddHHmmss").format(LocalDateTime.now()));

        // Khởi tạo hoặc cập nhật đơn hàng trong DB để cơ chế Polling & Webhook hoạt động tự động
        CustomerOrder o = orders.findByCode(tempCode).orElseGet(() -> {
            CustomerOrder co = new CustomerOrder();
            co.code = tempCode;
            co.channel = "POS";
            co.status = Enums.OrderStatus.PENDING;
            co.paymentStatus = Enums.PaymentStatus.UNPAID;
            co.createdAt = LocalDateTime.now();
            return co;
        });
        o.total = amount;
        o.subtotal = amount;
        if ("VNPAY".equalsIgnoreCase(method)) {
            o.paymentMethod = Enums.PaymentMethod.VNPAY;
        } else if ("MOMO".equalsIgnoreCase(method)) {
            o.paymentMethod = Enums.PaymentMethod.MOMO;
        } else {
            o.paymentMethod = Enums.PaymentMethod.BANK_QR;
        }
        orders.save(o);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", o.code);
        m.put("amount", amount);
        m.put("method", method);

        // Tài khoản nhận lấy từ cấu hình BANK_BIN / BANK_ACCOUNT_NO / BANK_ACCOUNT_NAME (cùng nguồn với web)
        String vietinQr = paymentProps.vietQrUrl(o.code, amount);
        m.put("vietinbankQr", vietinQr);
        m.put("bankName", (paymentProps.bankName == null || paymentProps.bankName.isBlank() ? "VietinBank" : paymentProps.bankName));
        m.put("bankBin", paymentProps.bankBin);
        m.put("accountNo", paymentProps.bankAccountNo);
        m.put("accountHolder", paymentProps.bankAccountName);

        try {
            if ("VNPAY".equalsIgnoreCase(method)) {
                m.put("paymentUrl", vnPayService.createPaymentUrl(o, amount, "127.0.0.1"));
                m.put("qrCodeUrl", vietinQr);
                m.put("isGatewayUrl", true);
            } else if ("MOMO".equalsIgnoreCase(method)) {
                var momoRes = moMoService.createPayment(o, amount);
                m.put("paymentUrl", momoRes.get("payUrl"));
                m.put("qrCodeUrl", momoRes.get("qrCodeUrl"));
                m.put("deeplink", momoRes.get("deeplink"));
                m.put("isGatewayUrl", true);
            } else {
                m.put("paymentUrl", vietinQr);
                m.put("qrCodeUrl", vietinQr);
                m.put("isGatewayUrl", false);
            }
        } catch (Exception e) {
            throw ApiException.bad("Không tạo được giao dịch " + method + ": " + e.getMessage());
        }
        return m;
    }

    /** Danh sách hóa đơn bán hàng tại quầy. */
    @GetMapping("/orders")
    public List<Map<String, Object>> getPosOrders(@RequestParam(required = false) String date,
                                                  @RequestParam(required = false) Long staffId) {
        var list = orders.findByChannelOrderByCreatedAtDesc("POS");
        return list.stream()
                .filter(o -> {
                    if (date != null && !date.isBlank()) {
                        return o.createdAt != null && o.createdAt.toLocalDate().toString().equals(date);
                    }
                    return true;
                })
                .filter(o -> staffId == null || (o.staffId != null && o.staffId.equals(staffId)))
                .map(o -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", o.id);
                    m.put("code", o.code);
                    m.put("createdAt", o.createdAt);
                    m.put("buyerName", o.buyerName);
                    m.put("phone", o.phone);
                    m.put("staffId", o.staffId);
                    m.put("staffName", o.staffName);
                    m.put("total", o.total);
                    m.put("posPaymentMethod", o.posPaymentMethod != null ? o.posPaymentMethod : o.paymentMethod.name());
                    m.put("cashGiven", o.cashGiven);
                    m.put("cashChange", o.cashChange);
                    m.put("commissionAmount", o.commissionAmount);
                    m.put("itemCount", o.items.stream().mapToInt(i -> i.quantity).sum());
                    m.put("items", o.items.stream().map(i -> Map.of(
                            "productName", i.productName,
                            "variantName", i.variantName != null ? i.variantName : "",
                            "quantity", i.quantity,
                            "unitPrice", i.unitPrice
                    )).toList());
                    return m;
                }).toList();
    }

    /** Thông tin ca bán hàng hiện tại. */
    @GetMapping("/shifts/current")
    public Map<String, Object> getCurrentShift() {
        var shiftOpt = shifts.findFirstByStatusOrderByStartTimeDesc("OPEN");
        if (shiftOpt.isEmpty()) {
            return Map.of("open", false);
        }
        PosShift s = shiftOpt.get();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("open", true);
        m.put("id", s.id);
        m.put("shiftCode", s.shiftCode);
        m.put("staffId", s.staffId);
        m.put("staffName", s.staffName);
        m.put("startTime", s.startTime);
        m.put("openingCash", s.openingCash);
        m.put("expectedCash", s.expectedCash);
        m.put("totalSales", s.totalSales);
        m.put("orderCount", s.orderCount);
        m.put("itemCount", s.itemCount);
        m.put("cashSales", s.cashSales);
        m.put("bankQrSales", s.bankQrSales);
        m.put("cardSales", s.cardSales);
        m.put("notes", s.notes != null ? s.notes : "");
        return m;
    }

    /** Mở ca thu ngân mới. */
    @PostMapping("/shifts/open")
    public PosShift openShift(@RequestBody Map<String, Object> body) {
        var existing = shifts.findFirstByStatusOrderByStartTimeDesc("OPEN");
        if (existing.isPresent()) {
            return existing.get();
        }

        User cur = Current.user();
        PosShift s = new PosShift();
        s.shiftCode = "CA-" + DateTimeFormatter.ofPattern("yyMMdd-HHmm").format(LocalDateTime.now());
        s.staffId = cur != null ? cur.id : null;
        s.staffName = cur != null ? cur.fullName : "Thu ngân chính";
        s.staffEmail = cur != null ? cur.email : "";
        s.startTime = LocalDateTime.now();

        long openingCash = 0;
        if (body.get("openingCash") != null) {
            openingCash = Long.parseLong(body.get("openingCash").toString());
        }
        s.openingCash = openingCash;
        s.expectedCash = openingCash;
        s.status = "OPEN";
        s.notes = body.get("notes") != null ? body.get("notes").toString() : "Mở ca đầu ngày";

        return shifts.save(s);
    }

    /** Đóng ca thu ngân và bàn giao két. */
    @PostMapping("/shifts/close")
    @Transactional
    public PosShift closeShift(@RequestBody Map<String, Object> body) {
        PosShift s = shifts.findFirstByStatusOrderByStartTimeDesc("OPEN")
                .orElseThrow(() -> ApiException.bad("Không có ca bán hàng nào đang mở"));

        long closingCash = 0;
        if (body.get("closingCash") != null) {
            closingCash = Long.parseLong(body.get("closingCash").toString());
        }
        s.closingCash = closingCash;
        s.cashDifference = closingCash - s.expectedCash;
        s.endTime = LocalDateTime.now();
        s.status = "CLOSED";
        if (body.get("notes") != null) {
            s.notes = (s.notes != null ? s.notes + "\n" : "") + "Chốt ca: " + body.get("notes").toString();
        }

        return shifts.save(s);
    }

    /** Danh sách lịch sử các ca làm việc. */
    @GetMapping("/shifts")
    public List<PosShift> listShifts() {
        return shifts.findAllByOrderByStartTimeDesc();
    }

    /** Thống kê nhanh trong ngày tại quầy showroom. */
    @GetMapping("/summary-today")
    public Map<String, Object> summaryToday() {
        LocalDate today = LocalDate.now();
        var posOrders = orders.findByChannelOrderByCreatedAtDesc("POS").stream()
                .filter(o -> o.createdAt != null && o.createdAt.toLocalDate().equals(today))
                .toList();

        long todayRevenue = posOrders.stream().mapToLong(o -> o.total).sum();
        long cashRevenue = posOrders.stream()
                .filter(o -> "CASH".equalsIgnoreCase(o.posPaymentMethod) || o.paymentMethod == Enums.PaymentMethod.COD)
                .mapToLong(o -> o.total).sum();
        long bankQrRevenue = posOrders.stream()
                .filter(o -> "BANK_QR".equalsIgnoreCase(o.posPaymentMethod) || o.paymentMethod == Enums.PaymentMethod.BANK_QR)
                .mapToLong(o -> o.total).sum();
        long cardRevenue = posOrders.stream()
                .filter(o -> "POS_CARD".equalsIgnoreCase(o.posPaymentMethod) || "CARD".equalsIgnoreCase(o.posPaymentMethod) || o.paymentMethod == Enums.PaymentMethod.BANK_QR)
                .mapToLong(o -> o.total).sum();
        int todayOrders = posOrders.size();
        int todayItems = posOrders.stream().flatMap(o -> o.items.stream()).mapToInt(i -> i.quantity).sum();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date", today.toString());
        m.put("todayRevenue", todayRevenue);
        m.put("cashRevenue", cashRevenue);
        m.put("bankQrRevenue", bankQrRevenue);
        m.put("cardRevenue", cardRevenue);
        m.put("todayOrders", todayOrders);
        m.put("todayItems", todayItems);
        return m;
    }

    /** Danh sách nhân viên bán hàng trực tiếp phục vụ chọn thu ngân và tư vấn tại quầy POS. */
    @GetMapping("/staff-list")
    public List<Map<String, Object>> getStaffList() {
        return users.findAll().stream()
                .filter(u -> u.role == Enums.Role.STAFF || u.role == Enums.Role.ADMIN)
                .filter(u -> !"TERMINATED".equalsIgnoreCase(u.employmentStatus) && !"LOCKED".equalsIgnoreCase(u.employmentStatus))
                .sorted(Comparator.comparing(u -> u.role == Enums.Role.STAFF ? 0 : 1))
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", u.id);
                    m.put("fullName", u.fullName);
                    m.put("email", u.email);
                    m.put("phone", u.phone);
                    m.put("role", u.role.name());
                    m.put("staffCode", u.staffCode != null ? u.staffCode : "NV-" + u.id);
                    m.put("department", u.department != null ? u.department : (u.role == Enums.Role.ADMIN ? "Quản lý Showroom" : "Tư vấn bán hàng trực tiếp"));
                    m.put("commissionRate", u.commissionRate != null ? u.commissionRate : 2.0);
                    m.put("monthlySalesTarget", u.monthlySalesTarget != null ? u.monthlySalesTarget : 50_000_000L);
                    return m;
                }).toList();
    }

    /** Hiệu suất và hoa hồng cá nhân của nhân viên bán hàng trực tiếp đang đăng nhập. */
    @GetMapping("/my-stats")
    public Map<String, Object> getMyStats() {
        User cur = Current.user();
        if (cur == null) {
            throw ApiException.bad("Chưa đăng nhập");
        }
        Long staffId = cur.id;
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.now();
        LocalDate startOfMonth = thisMonth.atDay(1);
        LocalDate endOfMonth = thisMonth.atEndOfMonth();

        var allPosOrders = orders.findByChannelOrderByCreatedAtDesc("POS").stream()
                .filter(o -> o.staffId != null && o.staffId.equals(staffId))
                .toList();

        // Hôm nay
        var todayOrders = allPosOrders.stream()
                .filter(o -> o.createdAt != null && o.createdAt.toLocalDate().equals(today))
                .toList();
        long todaySales = todayOrders.stream().mapToLong(o -> o.total).sum();
        int todayOrderCount = todayOrders.size();
        int todayItemCount = todayOrders.stream().flatMap(o -> o.items.stream()).mapToInt(i -> i.quantity).sum();

        // Tháng này
        var monthOrders = allPosOrders.stream()
                .filter(o -> o.createdAt != null && !o.createdAt.toLocalDate().isBefore(startOfMonth) && !o.createdAt.toLocalDate().isAfter(endOfMonth))
                .toList();
        long monthSales = monthOrders.stream().mapToLong(o -> o.total).sum();
        int monthOrderCount = monthOrders.size();
        long target = cur.monthlySalesTarget != null && cur.monthlySalesTarget > 0 ? cur.monthlySalesTarget : 50_000_000L;
        double targetProgress = Math.round((monthSales * 1000.0) / target) / 10.0;

        // Thống kê hoa hồng
        var commTxs = commissionTxRepo.findByStaffIdOrderByCreatedAtDesc(staffId);
        long totalEarned = commTxs.stream().filter(t -> "EARN".equalsIgnoreCase(t.txType)).mapToLong(t -> t.amount).sum();
        long totalPayoutTxs = commTxs.stream().filter(t -> "PAYOUT".equalsIgnoreCase(t.txType)).mapToLong(t -> t.amount).sum();
        long totalPaid = (cur.totalCommissionPaid != null ? cur.totalCommissionPaid : 0L) + totalPayoutTxs;
        long pendingCommission = Math.max(0, totalEarned - totalPaid);

        // Ca làm việc gần nhất của nhân viên
        var myShifts = shifts.findAllByOrderByStartTimeDesc().stream()
                .filter(s -> s.staffId != null && s.staffId.equals(staffId))
                .limit(5)
                .toList();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("staffId", cur.id);
        m.put("fullName", cur.fullName);
        m.put("email", cur.email);
        m.put("phone", cur.phone);
        m.put("role", cur.role.name());
        m.put("staffCode", cur.staffCode != null ? cur.staffCode : "NV-" + cur.id);
        m.put("department", cur.department != null ? cur.department : "Tư vấn bán hàng trực tiếp");
        m.put("commissionRate", cur.commissionRate != null ? cur.commissionRate : 2.0);
        m.put("monthlySalesTarget", target);
        m.put("todaySales", todaySales);
        m.put("todayOrderCount", todayOrderCount);
        m.put("todayItemCount", todayItemCount);
        m.put("monthSales", monthSales);
        m.put("monthOrderCount", monthOrderCount);
        m.put("targetProgress", targetProgress);
        m.put("totalEarnedCommission", totalEarned);
        m.put("totalPaidCommission", totalPaid);
        m.put("pendingCommission", pendingCommission);
        m.put("recentTransactions", commTxs.stream().limit(20).map(t -> {
            Map<String, Object> tm = new LinkedHashMap<>();
            tm.put("id", t.id);
            tm.put("txType", t.txType);
            tm.put("orderCode", t.orderCode);
            tm.put("amount", t.amount);
            tm.put("rate", t.rate);
            tm.put("note", t.note);
            tm.put("createdAt", t.createdAt);
            return tm;
        }).toList());
        m.put("recentShifts", myShifts);

        return m;
    }
}
