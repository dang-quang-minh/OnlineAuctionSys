package com.auction.app.common.dto;

import java.io.Serializable;

public record ProductInputDTO(
        String type,
        String name,
        double startingPrice,
        String artist,
        String origin,
        Integer manufacturingYear) implements Serializable {
}
