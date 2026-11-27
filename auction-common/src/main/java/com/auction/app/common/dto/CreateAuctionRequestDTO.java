package com.auction.app.common.dto;

import java.io.Serial;
import java.io.Serializable;

public record CreateAuctionRequestDTO(
        String productId,
        long durationMinutes
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}