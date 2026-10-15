package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface SavedCartRepo extends JpaRepository<SavedCart, Long> { List<SavedCart> findByUserIdOrderByUpdatedAtDesc(Long uid); }
