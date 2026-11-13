package com.ares.shop.repo;

import com.ares.shop.model.StaffCommissionTx;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StaffCommissionTxRepo extends JpaRepository<StaffCommissionTx, Long> {
    List<StaffCommissionTx> findByStaffIdOrderByCreatedAtDesc(Long staffId);
    List<StaffCommissionTx> findAllByOrderByCreatedAtDesc();
}
