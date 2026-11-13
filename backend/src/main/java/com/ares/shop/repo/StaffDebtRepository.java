package com.ares.shop.repo;

import com.ares.shop.model.StaffDebt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StaffDebtRepository extends JpaRepository<StaffDebt, Long> {
    List<StaffDebt> findAllByOrderByCreatedAtDesc();
    List<StaffDebt> findByStaffIdOrderByCreatedAtDesc(Long staffId);
    List<StaffDebt> findByStatusOrderByCreatedAtDesc(String status);
}
