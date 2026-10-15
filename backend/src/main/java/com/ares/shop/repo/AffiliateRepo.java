package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface AffiliateRepo extends JpaRepository<Affiliate, Long> { Optional<Affiliate> findByUserId(Long uid); Optional<Affiliate> findByCodeIgnoreCase(String c); }
