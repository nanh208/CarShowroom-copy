package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface AlertRepo extends JpaRepository<WatchAlert, Long> { List<WatchAlert> findByUserId(Long uid); List<WatchAlert> findByFiredIsNullOrFiredFalse(); }
