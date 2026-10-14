package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface BidRepo extends JpaRepository<Bid, Long> {
    List<Bid> findTop30ByAuctionIdOrderByAmountDesc(Long auctionId);
    Optional<Bid> findFirstByAuctionIdOrderByAmountDesc(Long auctionId);
    List<Bid> findByUserIdOrderByCreatedAtDesc(Long userId);
}
