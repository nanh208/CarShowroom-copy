package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.ares.shop.service.AuctionService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
public class AuctionController {
    private final AuctionRepo auctions; private final BidRepo bids; private final AuctionService service;

    public AuctionController(AuctionRepo auctions, BidRepo bids, AuctionService service) {
        this.auctions = auctions; this.bids = bids; this.service = service;
    }

    @GetMapping("/api/auctions")
    public List<Auction> list() {
        return auctions.findAllByOrderByEndTimeAsc().stream().filter(a -> !a.cancelled)
                .sorted(Comparator.comparing((Auction a) -> a.getStatus().ordinal() == 1 ? 0 : a.getStatus().ordinal() == 0 ? 1 : 2))
                .toList();
    }

    @GetMapping("/api/auctions/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        Auction a = auctions.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy phiên đấu giá"));
        if (a.getStatus() == Enums.AuctionStatus.ENDED && !a.finalized) a = service.finalizeOne(a);
        User me = Current.userOrNull();
        var top = bids.findFirstByAuctionIdOrderByAmountDesc(id);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("auction", a);
        m.put("bids", bids.findTop30ByAuctionIdOrderByAmountDesc(id));
        m.put("serverTime", LocalDateTime.now());
        m.put("leading", me != null && top.isPresent() && top.get().user.id.equals(me.id));
        m.put("youWon", me != null && a.winner != null && a.winner.id.equals(me.id));
        return m;
    }

    public record BidReq(long amount) {}

    @PostMapping("/api/auctions/{id}/bids")
    public Bid bid(@PathVariable Long id, @RequestBody BidReq r) { return service.placeBid(id, Current.user(), r.amount()); }

    @GetMapping("/api/account/bids")
    public List<Map<String, Object>> myBids() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Bid b : bids.findByUserIdOrderByCreatedAtDesc(Current.user().id)) {
            out.add(Map.of("id", b.id, "amount", b.amount, "createdAt", b.createdAt, "auctionId", b.auction.id,
                    "auctionTitle", b.auction.title, "status", b.auction.getStatus(), "currentPrice", b.auction.currentPrice,
                    "won", b.auction.winner != null && b.auction.winner.id.equals(Current.user().id),
                    "orderCode", Objects.toString(b.auction.orderCode, "")));
        }
        return out;
    }

    // ===== Admin =====
    @GetMapping("/api/admin/auctions")
    public List<Auction> adminList() { return auctions.findAll(org.springframework.data.domain.Sort.by("id").descending()); }

    @PostMapping("/api/admin/auctions")
    public Auction create(@RequestBody Auction a) {
        a.id = null; a.bidCount = 0; a.currentPrice = a.startPrice; a.finalized = false; a.winner = null; a.orderCode = null;
        return auctions.save(validate(a));
    }

    @PutMapping("/api/admin/auctions/{id}")
    public Auction update(@PathVariable Long id, @RequestBody Auction in) {
        Auction a = auctions.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy"));
        a.title = in.title; a.brandName = in.brandName; a.year = in.year; a.mileage = in.mileage; a.description = in.description;
        a.images = in.images; a.specs = in.specs; a.minIncrement = in.minIncrement; a.endTime = in.endTime; a.startTime = in.startTime;
        if (a.bidCount == 0) { a.startPrice = in.startPrice; a.currentPrice = in.startPrice; }
        if (a.finalized && a.endTime != null && a.endTime.isAfter(LocalDateTime.now()) && a.winner == null) a.finalized = false;
        return auctions.save(validate(a));
    }

    @PostMapping("/api/admin/auctions/{id}/cancel")
    public Auction cancel(@PathVariable Long id) {
        Auction a = auctions.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy"));
        a.cancelled = true; return auctions.save(a);
    }

    @PostMapping("/api/admin/auctions/{id}/close")
    public Auction closeNow(@PathVariable Long id) {
        Auction a = auctions.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy"));
        a.endTime = LocalDateTime.now().minusSeconds(1); auctions.save(a);
        return service.finalizeOne(a);
    }

    @GetMapping("/api/admin/auctions/{id}/bids")
    public List<Map<String, Object>> adminBids(@PathVariable Long id) {
        return bids.findTop30ByAuctionIdOrderByAmountDesc(id).stream().map(b -> Map.<String, Object>of(
                "id", b.id, "amount", b.amount, "createdAt", b.createdAt, "name", Objects.toString(b.user.fullName, ""),
                "email", b.user.email)).toList();
    }

    private Auction validate(Auction a) {
        if (a.title == null || a.title.isBlank()) throw ApiException.bad("Tên xe bắt buộc");
        if (a.startPrice <= 0 || a.minIncrement <= 0) throw ApiException.bad("Giá khởi điểm và bước giá phải > 0");
        if (a.endTime == null) throw ApiException.bad("Thiếu thời gian kết thúc");
        if (a.startTime == null) a.startTime = LocalDateTime.now();
        if (!a.endTime.isAfter(a.startTime)) throw ApiException.bad("Thời gian kết thúc phải sau thời gian bắt đầu");
        return a;
    }
}
