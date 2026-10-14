package com.ares.shop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
public class TicketMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @com.fasterxml.jackson.annotation.JsonIgnore @ManyToOne public Ticket ticket; public boolean fromStaff; public String author; @Column(length = 3000) public String content; public LocalDateTime createdAt = LocalDateTime.now();
}
