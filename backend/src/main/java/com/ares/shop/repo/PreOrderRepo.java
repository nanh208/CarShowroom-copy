package com.ares.shop.repo;
import com.ares.shop.model.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface PreOrderRepo extends JpaRepository<PreOrder, Long> { List<PreOrder> findAllByOrderByCreatedAtDesc(); List<PreOrder> findByUserIdOrderByCreatedAtDesc(Long u); List<PreOrder> findByProductIdAndStatus(Long p, String s); }
