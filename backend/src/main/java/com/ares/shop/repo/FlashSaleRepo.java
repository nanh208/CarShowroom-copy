package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface FlashSaleRepo extends JpaRepository<FlashSale, Long> { List<FlashSale> findAllByOrderByStartTimeDesc(); List<FlashSale> findByProductId(Long pid); }
