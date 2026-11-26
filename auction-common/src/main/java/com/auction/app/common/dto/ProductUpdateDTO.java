package com.auction.app.common.dto;

import java.io.Serializable;

public record ProductUpdateDTO(
        String productId,
        ProductInputDTO product) implements Serializable {
}