package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface AbandonedCartRepo extends JpaRepository<AbandonedCart, Long> { Optional<AbandonedCart> findByUserId(Long uid); Optional<AbandonedCart> findByEmailIgnoreCase(String e); List<AbandonedCart> findAllByOrderByLastSeenDesc(); }
