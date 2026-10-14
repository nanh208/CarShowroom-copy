package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface BannerRepo extends JpaRepository<Banner, Long> { List<Banner> findByActiveTrueOrderBySortOrderAsc(); List<Banner> findAllByOrderByPositionAscSortOrderAsc(); }
