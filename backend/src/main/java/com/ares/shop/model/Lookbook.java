package com.ares.shop.model;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Bộ sưu tập / lookbook do shop tuyển chọn. */
@Entity
public class Lookbook {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true) public String slug; public String title; public String subtitle;
    @Column(length = 3000) public String description; public String cover; public String accent;
    @ElementCollection(fetch = FetchType.EAGER) @OrderColumn public List<Long> productIds = new ArrayList<>();
    public Boolean active = true; public Integer sortOrder = 0; public LocalDateTime createdAt = LocalDateTime.now();
}
