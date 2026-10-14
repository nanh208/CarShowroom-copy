package com.ares.shop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String sessionId;
    public String customerName;
    public boolean fromAdmin;
    @Column(length = 2000) public String content;
    public LocalDateTime createdAt = LocalDateTime.now();
}
