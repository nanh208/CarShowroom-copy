package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface ReviewVoteRepo extends JpaRepository<ReviewVote, Long> { Optional<ReviewVote> findByReviewIdAndUserId(Long r, Long u); long countByReviewIdAndHelpfulTrue(Long r); }
