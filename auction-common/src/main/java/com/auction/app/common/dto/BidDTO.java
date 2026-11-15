package com.auction.app.common.dto;

import java.io.Serializable;

public record BidDTO(String auctionId, double amount) implements Serializable {
}
