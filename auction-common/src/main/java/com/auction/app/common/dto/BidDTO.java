// BidDTO.java
package com.auction.app.common.dto;

import java.io.Serializable;
import java.time.Instant;

public record BidDTO(String bidId, String auctionId, String bidderId,
                     double amount, Instant bidTime) implements Serializable {
}