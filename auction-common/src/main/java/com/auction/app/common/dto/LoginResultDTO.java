package com.auction.app.common.dto;

import java.io.Serializable;
import java.util.Objects;

/** Kết quả đăng nhập do server gửi về client. */
public record LoginResultDTO(boolean success, String message, String userId, String role) implements Serializable {

    public LoginResultDTO(boolean success, String message) {
        this(success, message, null, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LoginResultDTO that)) {
            return false;
        }
        if (success != that.success || !Objects.equals(message, that.message)) {
            return false;
        }
        if ((this.userId == null && this.role == null) || (that.userId == null && that.role == null)) {
            return true;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, message);
    }
}
