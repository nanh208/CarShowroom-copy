package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface ChatRepo extends JpaRepository<ChatMessage, Long> { List<ChatMessage> findBySessionIdAndIdGreaterThanOrderByIdAsc(String s, Long after); @Query("select m.sessionId from ChatMessage m group by m.sessionId order by max(m.id) desc") List<String> sessions(); ChatMessage findFirstBySessionIdOrderByIdDesc(String s); ChatMessage findFirstBySessionIdAndFromAdminFalseOrderByIdAsc(String s); }
