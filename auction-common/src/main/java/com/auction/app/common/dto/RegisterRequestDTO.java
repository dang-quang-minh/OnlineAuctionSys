package com.auction.app.common.dto;

import java.io.Serializable;

/** Yêu cầu đăng ký tài khoản mới từ client. */
public record RegisterRequestDTO(String username, String email, String password, String role)
        implements Serializable {
}
