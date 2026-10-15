package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface GiftCardRepo extends JpaRepository<GiftCard, Long> { Optional<GiftCard> findByCodeIgnoreCase(String c); List<GiftCard> findByBuyerUserIdOrRedeemedByUserId(Long a, Long b); }
