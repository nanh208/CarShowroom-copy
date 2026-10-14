package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface TicketRepo extends JpaRepository<Ticket, Long> { List<Ticket> findByUserIdOrderByUpdatedAtDesc(Long uid); List<Ticket> findAllByOrderByUpdatedAtDesc(); Optional<Ticket> findByCode(String c); }
