package com.auction.app.common.dto;

import java.io.Serializable;

/** Yêu cầu mở phiên cho sản phẩm của người bán đã đăng nhập. */
public record AuctionCreateDTO(String productId, double startingPrice,
                               long durationSeconds) implements Serializable {
}
