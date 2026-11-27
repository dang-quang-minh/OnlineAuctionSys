package com.auction.app.common.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** Một mức giá được đặt bởi một người tham gia đấu giá. */
public final class Bid implements Serializable {
    private static final long serialVersionUID = 1L;

    // ID của lần đặt giá (immutable)
    private final String bidId;

    // ID của người đặt giá (immutable)
    private final String bidderId;

    // Mức giá được đặt (immutable)
    private final double amount;

    // Thời gian đặt giá (immutable)
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
