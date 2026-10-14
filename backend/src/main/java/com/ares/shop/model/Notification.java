package com.ares.shop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @JsonIgnore @ManyToOne(fetch = FetchType.LAZY) public User user;
    public String kind;      // ORDER, AUCTION, SYSTEM
    public String title;
    @Column(length = 1000) public String body;
    public String link;
    public boolean read;
    public LocalDateTime createdAt = LocalDateTime.now();
}
