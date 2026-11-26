package com.auction.app.common.dto;

import java.io.Serializable;

public record ProductDetailDTO(
        String productId,
        String sellerId,
        String name,
        double startingPrice,
        String type,
        String artist,
        String origin,
        Integer manufacturingYear) implements Serializable {
}

