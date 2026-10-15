package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface ReferralRepo extends JpaRepository<Referral, Long> { Optional<Referral> findByCodeIgnoreCase(String c); Optional<Referral> findByOwnerUserId(Long uid); }
