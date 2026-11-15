package com.auction.app.common.dto;

import com.auction.app.common.model.Auction;
import com.auction.app.common.model.AuctionStatus;
import java.io.Serializable;
import java.time.LocalDateTime;

public record AuctionDTO(String auctionId, String productName, double currentPrice,
        AuctionStatus status, LocalDateTime endTime) implements Serializable {

    public static AuctionDTO from(Auction auction) {
        auction.getLock().lock();
        try {
            auction.refreshStatus(LocalDateTime.now());
            return new AuctionDTO(auction.getAuctionId(), auction.getProduct().getName(),
                    auction.getCurrentPrice(), auction.getStatus(), auction.getEndTime());
        } finally {
            auction.getLock().unlock();
        }
    }
}
