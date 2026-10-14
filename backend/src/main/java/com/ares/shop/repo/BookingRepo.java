package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface BookingRepo extends JpaRepository<Booking, Long> { List<Booking> findByUserIdOrderByCreatedAtDesc(Long uid); List<Booking> findAllByOrderByCreatedAtDesc(); }
