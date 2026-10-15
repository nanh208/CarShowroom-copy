package com.ares.shop.repo;
import com.ares.shop.model.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface ProductViewRepo extends JpaRepository<ProductView, Long> { List<ProductView> findTop20ByUserIdOrderByViewedAtDesc(Long u); Optional<ProductView> findByUserIdAndProductId(Long u, Long p); List<ProductView> findByUserIdOrderByViewedAtDesc(Long u); }
