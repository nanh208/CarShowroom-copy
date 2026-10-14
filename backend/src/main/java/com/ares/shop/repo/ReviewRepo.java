package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface ReviewRepo extends JpaRepository<Review, Long> {
    List<Review> findByProductIdAndApprovedTrueOrderByCreatedAtDesc(Long pid);
    List<Review> findAllByOrderByCreatedAtDesc();
    List<Review> findByApprovedTrue();
    boolean existsByProductIdAndUserId(Long pid, Long uid);
}
