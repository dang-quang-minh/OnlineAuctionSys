package com.auction.app.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public final class Bid implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String bidId;
    private final String bidderId;
    private final double amount;
    private final LocalDateTime bidTime;

    public Bid(String bidId, String bidderId, double amount, LocalDateTime bidTime) {
        this.bidId = bidId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.bidTime = bidTime;
    }

    public String getBidId() {
        return bidId;
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
