package com.ares.shop.model;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Nhật ký hội thoại với trợ lý ảo (để xem lại & cải thiện ý định). */
@Entity @Table(indexes = @Index(columnList = "session"))
public class BotLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String session; public Long userId; @Column(length = 1000) public String question; public String intent;
    @Column(length = 4000) public String answer; public Boolean matched; public LocalDateTime createdAt = LocalDateTime.now();
}
