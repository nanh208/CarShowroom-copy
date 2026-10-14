package com.ares.shop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
public class ReturnRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"items","timeline","user"}) @ManyToOne(fetch = FetchType.EAGER) public CustomerOrder order; public String orderCode; @com.fasterxml.jackson.annotation.JsonIgnore @ManyToOne(fetch = FetchType.EAGER) public User user; public String customerName; public String kind = "RETURN"; @Column(length = 1000) public String reason; @ElementCollection(fetch = FetchType.EAGER) @OrderColumn public List<String> images = new ArrayList<>(); public long refundAmount; @Enumerated(EnumType.STRING) public Enums.ReturnStatus status = Enums.ReturnStatus.PENDING; @Column(length = 1000) public String adminNote; public LocalDateTime createdAt = LocalDateTime.now(); public LocalDateTime updatedAt;
}
