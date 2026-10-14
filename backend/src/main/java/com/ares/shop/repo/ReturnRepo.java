package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface ReturnRepo extends JpaRepository<ReturnRequest, Long> { List<ReturnRequest> findByUserIdOrderByCreatedAtDesc(Long uid); List<ReturnRequest> findAllByOrderByCreatedAtDesc(); List<ReturnRequest> findByOrderCode(String c); }
