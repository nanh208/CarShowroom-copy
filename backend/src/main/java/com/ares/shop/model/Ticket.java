package com.ares.shop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true) public String code; @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"password","provider","locked","createdAt","points","tier","avatarUrl","role"}) @ManyToOne(fetch = FetchType.EAGER) public User user; public String subject; public String category; public String orderCode; @Enumerated(EnumType.STRING) public Enums.TicketStatus status = Enums.TicketStatus.OPEN; @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER) @OrderBy("id") public List<TicketMessage> messages = new ArrayList<>(); public LocalDateTime createdAt = LocalDateTime.now(); public LocalDateTime updatedAt = LocalDateTime.now();
}
