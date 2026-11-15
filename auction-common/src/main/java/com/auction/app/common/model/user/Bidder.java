package com.auction.app.common.model.user;

/** Người dùng có thể tham gia trả giá. */
public class Bidder extends User {
    public Bidder(String id, String username, String password, String email) {
        super(id, username, password, email);
    }

    @Override
    public String getRole() {
        return "BIDDER";
    }
}
