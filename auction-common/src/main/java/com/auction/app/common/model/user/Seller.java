package com.auction.app.common.model.user;

public class Seller extends User {
    private static final long serialVersionUID = 1L;

    public Seller(String id, String username, String passwordHash, String email) {
        super(id, username, passwordHash, email);
    }

    @Override
    public String getRole() {
        return "SELLER";
    }
}
