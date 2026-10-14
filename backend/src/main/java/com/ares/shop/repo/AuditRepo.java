package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface AuditRepo extends JpaRepository<AuditLog, Long> { List<AuditLog> findTop300ByOrderByIdDesc(); }
