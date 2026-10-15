package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface CommissionRepo extends JpaRepository<AffiliateCommission, Long> {
    List<AffiliateCommission> findByAffiliateIdOrderByIdDesc(Long a); List<AffiliateCommission> findAllByOrderByIdDesc(); List<AffiliateCommission> findByStatus(String s);
    Optional<AffiliateCommission> findByOrderCode(String c);
}
