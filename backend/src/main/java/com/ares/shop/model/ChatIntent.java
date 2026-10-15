package com.ares.shop.model;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Ý định chatbot do admin cấu hình: từ khoá (phân tách dấu phẩy, không dấu cũng được) → câu trả lời + gợi ý nhanh. */
@Entity
public class ChatIntent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String name;
    @Column(length = 1000) public String keywords;
    @Column(length = 3000) public String answer;
    @Column(length = 500) public String quickReplies;
    /** Bản tiếng Anh (tuỳ chọn) — dùng khi khách chọn EN; trống thì lấy từ điển _bot hoặc giữ tiếng Việt. */
    @Column(length = 3000) public String answerEn; @Column(length = 500) public String quickRepliesEn;
    public Boolean active = true; public Integer hits = 0; public Integer sortOrder = 0;
    public LocalDateTime updatedAt = LocalDateTime.now();
}
