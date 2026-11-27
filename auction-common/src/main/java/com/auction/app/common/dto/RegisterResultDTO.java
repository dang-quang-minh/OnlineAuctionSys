package com.auction.app.common.dto;

import java.io.Serializable;

/** Kết quả đăng ký tài khoản do server gửi về client. */
public record RegisterResultDTO(boolean success, String message) implements Serializable {
}
