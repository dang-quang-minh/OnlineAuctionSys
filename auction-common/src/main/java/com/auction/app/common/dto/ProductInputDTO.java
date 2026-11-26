package com.auction.app.common.dto;

import java.io.Serializable;

public record ProductInputDTO(
        String name,
        double startingPrice,
        String type,
        String artist,
        String origin,
        Integer manufacturingYear) implements Serializable {
}
