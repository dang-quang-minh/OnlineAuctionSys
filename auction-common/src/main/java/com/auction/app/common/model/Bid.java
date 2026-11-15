package com.auction.app.common.model;

import java.time.LocalDateTime;
import java.util.Objects;

/** Một mức giá được đặt bởi một người tham gia đấu giá. */
public final class Bid {
    private final String bidderId;
    private final double amount;
    private final LocalDateTime bidTime;

    public Bid(String bidderId, double amount, LocalDateTime bidTime) {
        this.bidderId = Objects.requireNonNull(bidderId, "bidderId");
        if (!Double.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("Giá bid phải là số hữu hạn và không âm.");
        }
        this.amount = amount;
        this.bidTime = Objects.requireNonNull(bidTime, "bidTime");
    }

    public Bid(String bidderId, double amount) {
        this(bidderId, amount, LocalDateTime.now());
    }

    public String getBidderId() {
        return bidderId;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }
}
