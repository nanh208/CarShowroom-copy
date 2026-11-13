package com.ares.shop.repo;

import com.ares.shop.model.PosShift;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PosShiftRepo extends JpaRepository<PosShift, Long> {
    Optional<PosShift> findFirstByStaffIdAndStatusOrderByStartTimeDesc(Long staffId, String status);
    Optional<PosShift> findFirstByStatusOrderByStartTimeDesc(String status);
    List<PosShift> findAllByOrderByStartTimeDesc();
}
