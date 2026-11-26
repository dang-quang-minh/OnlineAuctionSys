package com.auction.app.common.dto;

import java.io.Serializable;
import java.time.Instant;

/** Kết quả cuối cùng của một phiên đấu giá đã kết thúc. */
public record AuctionEndDTO(String auctionId, String winnerBidderId,
                            double finalPrice, Instant endedAt) implements Serializable {
}
