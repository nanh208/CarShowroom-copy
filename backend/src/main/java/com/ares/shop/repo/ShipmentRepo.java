package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface ShipmentRepo extends JpaRepository<Shipment, Long> { Optional<Shipment> findByOrderId(Long id); Optional<Shipment> findByTrackingNo(String t); Optional<Shipment> findByOrderCode(String c); }
