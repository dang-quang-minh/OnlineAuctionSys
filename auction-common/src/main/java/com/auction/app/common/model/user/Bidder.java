package com.auction.app.common.model.user;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;

    public Bidder(String id, String username, String passwordHash, String email) {
        super(id, username, passwordHash, email);
    }

    @Override
    public String getRole() {
        return "BIDDER";
    }
}
