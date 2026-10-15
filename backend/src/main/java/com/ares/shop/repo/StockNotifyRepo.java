package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface StockNotifyRepo extends JpaRepository<StockNotify, Long> { List<StockNotify> findBySentIsNullOrSentFalse(); boolean existsByProductIdAndEmailIgnoreCaseAndSentIsNull(Long p, String e); }
