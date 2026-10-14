package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface CategoryRepo extends JpaRepository<Category, Long> { Optional<Category> findByCode(String c); List<Category> findAllByOrderBySortOrderAsc(); }
