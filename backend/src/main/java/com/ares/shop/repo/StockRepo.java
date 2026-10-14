package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface StockRepo extends JpaRepository<StockMovement, Long> { List<StockMovement> findTop200ByOrderByCreatedAtDesc(); }
