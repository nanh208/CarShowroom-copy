package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface BundleRepo extends JpaRepository<Bundle, Long> { List<Bundle> findByActiveTrueOrderBySortOrderAsc(); }
