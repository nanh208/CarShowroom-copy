package com.ares.shop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Subscriber {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true) public String email;
    public LocalDateTime createdAt = LocalDateTime.now();
}
