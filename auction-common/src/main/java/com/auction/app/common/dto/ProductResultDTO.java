package com.auction.app.common.dto;

import java.io.Serializable;

public record ProductResultDTO(
        String productId,
        boolean success,
        String message) implements Serializable {
}