package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface PriceHistoryRepo extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findByProductIdOrderByAtAsc(Long pid);
    Optional<PriceHistory> findTopByProductIdOrderByIdDesc(Long pid);
}
