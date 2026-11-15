package com.auction.app.common.model.user;

import java.util.Objects;

/** Thông tin chung của người dùng trong hệ thống đấu giá. */
public abstract class User {
    private final String id;
    private final String username;
    private final String password;
    private final String email;

    protected User(String id, String username, String password, String email) {
        this.id = Objects.requireNonNull(id, "id");
        this.username = Objects.requireNonNull(username, "username");
        this.password = Objects.requireNonNull(password, "password");
        this.email = Objects.requireNonNull(email, "email");
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    /** Trả về vai trò cụ thể của loại người dùng. */
    public abstract String getRole();
}
