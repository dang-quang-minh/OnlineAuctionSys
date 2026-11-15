package com.auction.app.common.model.user;

/** Người dùng quản trị hệ thống. */
public class Admin extends User {
    public Admin(String id, String username, String password, String email) {
        super(id, username, password, email);
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }
}
