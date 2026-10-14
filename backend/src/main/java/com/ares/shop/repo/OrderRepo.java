package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface OrderRepo extends JpaRepository<CustomerOrder, Long> { List<CustomerOrder> findByUserIdOrderByCreatedAtDesc(Long uid); List<CustomerOrder> findAllByOrderByCreatedAtDesc(); Optional<CustomerOrder> findByCode(String c); }
