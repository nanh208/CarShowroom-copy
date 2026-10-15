package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface PurchaseOrderRepo extends JpaRepository<PurchaseOrder, Long> { List<PurchaseOrder> findAllByOrderByCreatedAtDesc(); }
