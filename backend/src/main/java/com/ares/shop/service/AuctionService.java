package com.ares.shop.service;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.ares.shop.web.ApiException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuctionService {
    private final AuctionRepo auctions; private final BidRepo bids; private final OrderService orders;

    private final NotifyService notify;
    public AuctionService(AuctionRepo auctions, BidRepo bids, OrderService orders, NotifyService notify) {
        this.notify = notify;
        this.auctions = auctions; this.bids = bids; this.orders = orders;
    }

    public static String mask(String name) {
        if (name == null || name.isBlank()) return "Ẩn danh";
        String[] parts = name.trim().split("\\s+");
        String last = parts[parts.length - 1];
        return (parts.length > 1 ? parts[0].charAt(0) + ". " : "") + last.charAt(0) + "***";
    }

    @Transactional
    public synchronized Bid placeBid(Long auctionId, User user, long amount) {
        Auction a = auctions.findById(auctionId).orElseThrow(() -> ApiException.notFound("Không tìm thấy phiên đấu giá"));
        if (a.getStatus() != Enums.AuctionStatus.LIVE) throw ApiException.bad("Phiên đấu giá không diễn ra");
        if (amount < a.getNextMinBid()) throw ApiException.bad("Giá đặt tối thiểu là " + String.format("%,d", a.getNextMinBid()) + "đ");
        Optional<Bid> top = bids.findFirstByAuctionIdOrderByAmountDesc(auctionId);
        if (top.isPresent() && top.get().user.id.equals(user.id)) throw ApiException.bad("Bạn đang là người trả giá cao nhất");
        Bid b = new Bid(); b.auction = a; b.user = user; b.amount = amount; b.bidderName = mask(user.fullName);
        a.currentPrice = amount; a.bidCount++;
        // chống "bắn tỉa": đặt giá trong 2 phút cuối -> gia hạn thêm 2 phút
        if (a.endTime != null && a.endTime.minusMinutes(2).isBefore(LocalDateTime.now())) a.endTime = a.endTime.plusMinutes(2);
        auctions.save(a);
        top.ifPresent(t -> notify.send(t.user, "AUCTION", "Bạn đã bị trả giá cao hơn", a.title + ": giá mới " + String.format("%,d", amount) + "đ. Đặt giá lại để giữ vị trí dẫn đầu.", "/auctions/" + a.id));
        return bids.save(b);
    }

    /** Chốt các phiên đã kết thúc: tạo đơn đặt cọc cho người thắng. */
    @Scheduled(fixedDelay = 20_000)
    @Transactional
    public void finalizeEnded() {
        for (Auction a : auctions.findByFinalizedFalseAndCancelledFalse()) {
            if (a.getStatus() != Enums.AuctionStatus.ENDED) continue;
            finalizeOne(a);
        }
    }

    @Transactional
    public Auction finalizeOne(Auction a) {
        if (a.finalized) return a;
        bids.findFirstByAuctionIdOrderByAmountDesc(a.id).ifPresent(top -> {
            a.winner = top.user;
            a.orderCode = orders.auctionOrder(top.user, a.title, top.amount,
                    "Thắng đấu giá phiên #" + a.id + ". Vui lòng đặt cọc 10% trong 48 giờ.").code;
            notify.send(top.user, "AUCTION", "Chúc mừng! Bạn đã thắng đấu giá", a.title + " — vui lòng đặt cọc 10% trong 48 giờ.", "/orders/" + a.orderCode);
        });
        a.finalized = true;
        return auctions.save(a);
    }
}
