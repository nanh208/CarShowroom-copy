package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface BrandRepo extends JpaRepository<Brand, Long> { Optional<Brand> findByName(String n); }
