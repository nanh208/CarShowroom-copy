package com.ares.shop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
public class Banner {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String position = "HERO"; public String title; public String kicker; public String subtitle; public String image; public String link; public int sortOrder; public boolean active = true;
    /** Lịch hiển thị (null = luôn hiển thị). */
    public java.time.LocalDateTime startAt; public java.time.LocalDateTime endAt;
}
