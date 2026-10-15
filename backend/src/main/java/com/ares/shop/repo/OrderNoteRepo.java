package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface OrderNoteRepo extends JpaRepository<OrderNote, Long> { List<OrderNote> findByOrderIdOrderByCreatedAtAsc(Long id); }
