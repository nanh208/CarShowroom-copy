package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface ProductRepo extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> { List<Product> findTop8ByActiveTrueOrderBySoldDesc(); List<Product> findTop8ByActiveTrueAndIsNewTrueOrderByCreatedAtDesc(); List<Product> findTop4ByBrandIdAndIdNotAndActiveTrue(Long b, Long id); }
