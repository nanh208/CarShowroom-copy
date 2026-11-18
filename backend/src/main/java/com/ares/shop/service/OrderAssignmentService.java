package com.ares.shop.service;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderAssignmentService {
    private final OrderRepo orderRepo;
    private final UserRepo userRepo;
    private final SettingRepo settingRepo;
    private final StaffCommissionTxRepo commissionTxRepo;
    private final NotifyService notify;
    private final ObjectMapper json = new ObjectMapper();

    public static final String CONFIG_KEY = "orderAssignmentConfig";

    public OrderAssignmentService(OrderRepo orderRepo, UserRepo userRepo, SettingRepo settingRepo,
                                  StaffCommissionTxRepo commissionTxRepo, NotifyService notify) {
        this.orderRepo = orderRepo;
        this.userRepo = userRepo;
        this.settingRepo = settingRepo;
        this.commissionTxRepo = commissionTxRepo;
        this.notify = notify;
    }

    public static class Config {
        public boolean enabled = true;
        public String strategy = "ROUND_ROBIN"; // ROUND_ROBIN, LEAST_LOADED, TARGET_BASED
        public int lastIndex = 0;
        public List<Long> enabledStaffIds = new ArrayList<>(); // Rỗng = áp dụng cho mọi STAFF active
        public boolean autoNotify = true;
        public double defaultCommissionRate = 2.0;
    }

    public Config getConfig() {
        try {
            var s = settingRepo.findById(CONFIG_KEY);
            if (s.isPresent() && s.get().v != null && !s.get().v.isBlank()) {
                return json.readValue(s.get().v, Config.class);
            }
        } catch (Exception ignored) {}
        return new Config();
    }

    public synchronized void saveConfig(Config config) {
        try {
            AppSetting s = new AppSetting();
            s.k = CONFIG_KEY;
            s.v = json.writeValueAsString(config);
            settingRepo.save(s);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi lưu cấu hình chia đơn: " + e.getMessage());
        }
    }

    public List<User> getEligibleStaff(Config config) {
        return userRepo.findAll().stream()
                .filter(u -> (u.role == Enums.Role.STAFF || u.role == Enums.Role.ADMIN))
                .filter(u -> !u.locked)
                .filter(u -> !"TERMINATED".equalsIgnoreCase(u.employmentStatus))
                .filter(u -> u.autoAssignOrders == null || u.autoAssignOrders)
                .filter(u -> config.enabledStaffIds == null || config.enabledStaffIds.isEmpty() || config.enabledStaffIds.contains(u.id))
                .sorted(Comparator.comparing(u -> u.id))
                .toList();
    }

    /**
     * Tự động chia đơn cho nhân viên phù hợp dựa theo cấu hình và thuật toán đã chọn.
     */
    @Transactional
    public synchronized CustomerOrder assignOrder(CustomerOrder order) {
        return assignOrder(order, false);
    }

    @Transactional
    public synchronized CustomerOrder assignOrder(CustomerOrder order, boolean force) {
        if (order == null || order.staffId != null) return order;
        Config cfg = getConfig();
        if (!cfg.enabled && !force) return order;

        List<User> eligible = getEligibleStaff(cfg);
        if (eligible.isEmpty()) return order;

        User chosen = null;
        if ("LEAST_LOADED".equalsIgnoreCase(cfg.strategy)) {
            // Đếm số đơn đang PENDING, CONFIRMED, SHIPPING của từng nhân viên
            List<CustomerOrder> activeOrders = orderRepo.findAll().stream()
                    .filter(o -> o.staffId != null)
                    .filter(o -> o.status == Enums.OrderStatus.PENDING || o.status == Enums.OrderStatus.CONFIRMED || o.status == Enums.OrderStatus.SHIPPING)
                    .toList();
            Map<Long, Long> counts = new HashMap<>();
            for (User u : eligible) counts.put(u.id, 0L);
            for (CustomerOrder o : activeOrders) {
                if (counts.containsKey(o.staffId)) {
                    counts.put(o.staffId, counts.get(o.staffId) + 1);
                }
            }
            chosen = eligible.stream().min(Comparator.comparingLong(u -> counts.getOrDefault(u.id, 0L))).orElse(eligible.get(0));
        } else if ("TARGET_BASED".equalsIgnoreCase(cfg.strategy)) {
            // Ưu tiên nhân viên có tổng doanh số thấp hơn trong tháng
            List<CustomerOrder> all = orderRepo.findAll();
            Map<Long, Long> sales = new HashMap<>();
            for (User u : eligible) sales.put(u.id, 0L);
            for (CustomerOrder o : all) {
                if (o.staffId != null && sales.containsKey(o.staffId) && o.status != Enums.OrderStatus.CANCELLED) {
                    sales.put(o.staffId, sales.get(o.staffId) + o.total);
                }
            }
            chosen = eligible.stream().min(Comparator.comparingLong(u -> sales.getOrDefault(u.id, 0L))).orElse(eligible.get(0));
        } else {
            // Mặc định: ROUND_ROBIN xoay vòng
            int idx = cfg.lastIndex;
            if (idx >= eligible.size()) idx = 0;
            chosen = eligible.get(idx);
            cfg.lastIndex = (idx + 1) % eligible.size();
            saveConfig(cfg);
        }

        if (chosen != null) {
            order.staffId = chosen.id;
            order.staffName = chosen.fullName;
            double rate = chosen.commissionRate != null ? chosen.commissionRate : cfg.defaultCommissionRate;
            order.commissionRate = rate;
            order.commissionAmount = Math.round(order.total * (rate / 100.0));
            order.commissionStatus = "PENDING";
            order.timeline.put("ASSIGNED", LocalDateTime.now());

            CustomerOrder saved = orderRepo.save(order);

            // Ghi nhận giao dịch hoa hồng tạm tính
            if (commissionTxRepo != null && saved.commissionAmount != null && saved.commissionAmount > 0) {
                StaffCommissionTx tx = new StaffCommissionTx();
                tx.staffId = chosen.id;
                tx.staffName = chosen.fullName;
                tx.txType = "EARN";
                tx.orderId = saved.id;
                tx.orderCode = saved.code;
                tx.amount = saved.commissionAmount;
                tx.rate = rate;
                tx.note = "Hoa hồng đơn online " + saved.code + " (Tự động chia đơn theo " + cfg.strategy + ")";
                tx.createdBy = "AUTO_DISPATCH";
                tx.createdAt = LocalDateTime.now();
                commissionTxRepo.save(tx);
            }

            // Gửi thông báo đến tài khoản nhân viên
            if (cfg.autoNotify && notify != null) {
                try {
                    notify.send(chosen, "ORDER", "Đơn hàng mới được phân công tự động",
                            "Bạn được giao phụ trách đơn #" + saved.code + " (" + String.format("%,d", saved.total).replace(',', '.') + "đ). Vui lòng xử lý sớm!",
                            "/orders/" + saved.code);
                } catch (Exception ignored) {}
            }
            return saved;
        }

        return order;
    }

    /**
     * Chia tự động toàn bộ các đơn hàng chưa có nhân viên phụ trách.
     */
    @Transactional
    public synchronized Map<String, Object> dispatchAllUnassigned() {
        List<CustomerOrder> unassigned = orderRepo.findAll().stream()
                .filter(o -> o.staffId == null && o.status != Enums.OrderStatus.CANCELLED)
                .sorted(Comparator.comparing(o -> o.createdAt != null ? o.createdAt : LocalDateTime.MIN))
                .toList();

        int count = 0;
        List<Map<String, Object>> results = new ArrayList<>();
        for (CustomerOrder o : unassigned) {
            CustomerOrder assigned = assignOrder(o, true);
            if (assigned.staffId != null) {
                count++;
                results.add(Map.of("orderCode", assigned.code, "staffId", assigned.staffId, "staffName", assigned.staffName, "total", assigned.total));
            }
        }

        return Map.of(
                "success", true,
                "dispatchedCount", count,
                "totalUnassignedBefore", unassigned.size(),
                "details", results
        );
    }

    /**
     * Gán thủ công đơn hàng cho nhân viên được chỉ định.
     */
    @Transactional
    public CustomerOrder manualAssign(Long orderId, Long staffId) {
        CustomerOrder o = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        User staff = userRepo.findById(staffId).orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        o.staffId = staff.id;
        o.staffName = staff.fullName;
        double rate = staff.commissionRate != null ? staff.commissionRate : 2.0;
        o.commissionRate = rate;
        o.commissionAmount = Math.round(o.total * (rate / 100.0));
        o.commissionStatus = "PENDING";
        o.timeline.put("ASSIGNED_MANUAL", LocalDateTime.now());

        if (commissionTxRepo != null && o.commissionAmount > 0) {
            StaffCommissionTx tx = new StaffCommissionTx();
            tx.staffId = staff.id;
            tx.staffName = staff.fullName;
            tx.txType = "EARN";
            tx.orderId = o.id;
            tx.orderCode = o.code;
            tx.amount = o.commissionAmount;
            tx.rate = rate;
            tx.note = "Hoa hồng đơn hàng " + o.code + " (Quản lý phân công thủ công)";
            tx.createdBy = "MANAGER";
            tx.createdAt = LocalDateTime.now();
            commissionTxRepo.save(tx);
        }

        CustomerOrder saved = orderRepo.save(o);
        if (notify != null) {
            try {
                notify.send(staff, "ORDER", "Được phân công phụ trách đơn hàng",
                        "Quản lý đã gán bạn phụ trách đơn #" + o.code + " (" + String.format("%,d", o.total).replace(',', '.') + "đ).",
                        "/orders/" + o.code);
            } catch (Exception ignored) {}
        }
        return saved;
    }
}
