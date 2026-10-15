package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.time.LocalDateTime; import java.util.*;
public interface SubscriptionRepo extends JpaRepository<ProductSubscription, Long> {
    List<ProductSubscription> findByUserIdOrderByIdDesc(Long uid); List<ProductSubscription> findAllByOrderByIdDesc();
    List<ProductSubscription> findByStatusAndNextRunAtBefore(String status, LocalDateTime t);
}
