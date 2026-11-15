package com.auction.app.common.model.user;

/** Người dùng có thể đưa sản phẩm lên đấu giá. */
public class Seller extends User {
    public Seller(String id, String username, String password, String email) {
        super(id, username, password, email);
    }

    @Override
    public String getRole() {
        return "SELLER";
    }
}
