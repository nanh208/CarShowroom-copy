package com.ares.shop.repo;

import com.ares.shop.model.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface OrderRepo extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByUserIdOrderByCreatedAtDesc(Long uid);
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();
    Optional<CustomerOrder> findByCode(String c);
    /** Khoá dòng đơn hàng khi xác nhận thanh toán từ webhook/IPN (tránh 2 giao dịch cùng cộng tiền). */
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.code = :code")
    Optional<CustomerOrder> lockByCode(@org.springframework.data.repository.query.Param("code") String code);
    List<CustomerOrder> findByChannelOrderByCreatedAtDesc(String channel);
    List<CustomerOrder> findByStaffIdOrderByCreatedAtDesc(Long staffId);
    List<CustomerOrder> findByShiftIdOrderByCreatedAtDesc(Long shiftId);
}
