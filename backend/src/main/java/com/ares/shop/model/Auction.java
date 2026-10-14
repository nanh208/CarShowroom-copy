package com.ares.shop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

/** Phiên đấu giá xe hiếm. */
@Entity
public class Auction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String title;            // Ferrari F40 1990
    public String brandName;
    @Column(name = "model_year") public Integer year;
    public String mileage;          // "12.400 km"
    @Column(length = 3000) public String description;
    @ElementCollection(fetch = FetchType.EAGER) @OrderColumn
    public List<String> images = new ArrayList<>();
    @ElementCollection(fetch = FetchType.EAGER) @MapKeyColumn(name = "spec_key") @Column(name = "spec_value")
    public Map<String, String> specs = new LinkedHashMap<>();
    public long startPrice;
    public long minIncrement;
    public long currentPrice;
    public int bidCount;
    public LocalDateTime startTime;
    public LocalDateTime endTime;
    public boolean cancelled;
    public boolean finalized;
    @JsonIgnoreProperties({"password", "phone", "email", "provider", "locked", "role", "createdAt", "avatarUrl"})
    @ManyToOne(fetch = FetchType.EAGER) public User winner;
    public String orderCode;
    public LocalDateTime createdAt = LocalDateTime.now();

    public Enums.AuctionStatus getStatus() {
        LocalDateTime now = LocalDateTime.now();
        if (cancelled) return Enums.AuctionStatus.CANCELLED;
        if (startTime != null && now.isBefore(startTime)) return Enums.AuctionStatus.SCHEDULED;
        if (endTime != null && !now.isBefore(endTime)) return Enums.AuctionStatus.ENDED;
        return Enums.AuctionStatus.LIVE;
    }

    public long getNextMinBid() { return bidCount == 0 ? startPrice : currentPrice + minIncrement; }
}
