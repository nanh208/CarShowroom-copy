package com.ares.shop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long productId;
    public String productName;
    @JsonIgnore @ManyToOne(fetch = FetchType.EAGER) public User user;
    public String authorName;
    public int rating;
    @Column(length = 2000) public String comment;
    public boolean approved = true;
    public LocalDateTime createdAt = LocalDateTime.now();
}
