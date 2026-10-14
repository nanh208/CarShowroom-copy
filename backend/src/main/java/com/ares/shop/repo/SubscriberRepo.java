package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*;
public interface SubscriberRepo extends JpaRepository<Subscriber, Long> { boolean existsByEmailIgnoreCase(String email); }
