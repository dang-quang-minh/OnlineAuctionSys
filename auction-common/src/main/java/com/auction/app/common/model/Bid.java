package com.auction.app.common.model;

import java.time.Instant;
import java.util.Objects;

/** Một mức giá được đặt bởi một người tham gia đấu giá. */
public final class Bid {

    private final  String bidId;
    private final  String bidderId;
    private final  double amount;
    private final Instant bidTime;

    public Bid(String bidId, String bidderId, double amount, Instant bidTime) {
        this.bidId = Objects.requireNonNull(bidId, "bidId");
        this.bidderId = Objects.requireNonNull(bidderId, "bidderId");

        if (!Double.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException(
                    "Giá bid phải là số hữu hạn và không âm."
            );
        }

        this.amount = amount;
        this.bidTime = Objects.requireNonNull(bidTime, "bidTime");
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

    public Instant getBidTime() {
        return bidTime;
    }
}
