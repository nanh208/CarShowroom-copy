package com.ares.shop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

/** Bài viết CMS: tin tức, blog, video review. Nội dung HTML đã được làm sạch phía server (jsoup Safelist). */
@Entity
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true, nullable = false) public String slug;
    @Column(nullable = false) public String title;
    public String titleEn;
    @Column(length = 500) public String excerpt;
    @Column(length = 500) public String excerptEn;
    @Lob @Column(columnDefinition = "CLOB") public String contentHtml;
    @Lob @Column(columnDefinition = "CLOB") public String contentHtmlEn;
    public String coverImage;
    public String kind = "BLOG";            // NEWS | BLOG | VIDEO
    public String videoUrl;                 // YouTube/Vimeo/mp4 cho VIDEO
    @ElementCollection(fetch = FetchType.EAGER) @CollectionTable(name = "post_tags") @Column(name = "tag") public Set<String> tags = new LinkedHashSet<>();
    @ElementCollection(fetch = FetchType.EAGER) @CollectionTable(name = "post_products") @Column(name = "product_id") public List<Long> productIds = new ArrayList<>();
    public String status = "DRAFT";         // DRAFT | PUBLISHED
    public LocalDateTime publishedAt;
    public String author;
    public String seoTitle; @Column(length = 300) public String seoDescription;
    @Column(columnDefinition = "integer default 0") public int views;
    public LocalDateTime createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now();
    @Transient public int readMinutes;
}
