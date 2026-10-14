package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface PointRepo extends JpaRepository<PointTx, Long> { List<PointTx> findByUserIdOrderByCreatedAtDesc(Long uid); }
