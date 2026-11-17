package com.auction.app.common.dto;

import java.io.Serializable;

/** Kết quả đăng nhập do server gửi về client. */
public record LoginResultDTO(boolean success, String message) implements Serializable {
}
