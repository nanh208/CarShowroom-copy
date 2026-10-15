package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface EmailLogRepo extends JpaRepository<EmailLog, Long> { List<EmailLog> findTop200ByOrderByCreatedAtDesc(); List<EmailLog> findTop50ByToAddrIgnoreCaseOrderByCreatedAtDesc(String to); }
