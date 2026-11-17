// BidRequestDTO.java
package com.auction.app.common.dto;

import java.io.Serializable;

public record BidRequestDTO(String auctionId, String bidderId, double amount)
        implements Serializable {
}