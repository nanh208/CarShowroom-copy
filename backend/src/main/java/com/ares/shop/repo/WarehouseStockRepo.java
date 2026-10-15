package com.ares.shop.repo;
import com.ares.shop.model.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface WarehouseStockRepo extends JpaRepository<WarehouseStock, Long> { List<WarehouseStock> findByProductId(Long p); }
