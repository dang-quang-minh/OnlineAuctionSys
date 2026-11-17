// AuctionDTO.java
package com.auction.app.common.dto;

import com.auction.app.common.model.AuctionStatus;
import java.io.Serializable;
import java.time.Instant;

public record AuctionDTO(String auctionId, String productName, double currentPrice,
                         AuctionStatus status, Instant endsAt) implements Serializable {
}