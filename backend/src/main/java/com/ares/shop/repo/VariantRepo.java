package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface VariantRepo extends JpaRepository<ProductVariant, Long> { List<ProductVariant> findByProductIdOrderByIdAsc(Long pid); }
