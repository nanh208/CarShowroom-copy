package com.ares.shop.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
/** Nhật ký chạy tác vụ nền (@Scheduled). */
@Entity
public class JobRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String job; public LocalDateTime startedAt = LocalDateTime.now(); public Long ms; public Boolean ok; @Column(length = 1000) public String result; public String trigger;
}
