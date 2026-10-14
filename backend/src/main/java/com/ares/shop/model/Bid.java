package com.ares.shop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Bid {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @JsonIgnore @ManyToOne(optional = false) public Auction auction;
    @JsonIgnore @ManyToOne(optional = false) public User user;
    public String bidderName;   // tên đã che bớt
    public long amount;
    public LocalDateTime createdAt = LocalDateTime.now();
}
