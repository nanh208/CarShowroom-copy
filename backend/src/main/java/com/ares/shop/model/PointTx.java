package com.ares.shop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
public class PointTx {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @com.fasterxml.jackson.annotation.JsonIgnore @ManyToOne public User user; public int delta; public String reason; public LocalDateTime createdAt = LocalDateTime.now();
}
