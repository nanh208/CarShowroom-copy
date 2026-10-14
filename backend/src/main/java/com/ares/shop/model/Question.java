package com.ares.shop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
public class Question {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long productId; public String productName; @com.fasterxml.jackson.annotation.JsonIgnore @ManyToOne(fetch = FetchType.EAGER) public User user; public String authorName; @Column(length = 1000) public String question; @Column(length = 2000) public String answer; public String answeredBy; public LocalDateTime answeredAt; public boolean visible = true; public LocalDateTime createdAt = LocalDateTime.now();
}
