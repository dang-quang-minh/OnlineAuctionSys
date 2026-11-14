package com.auction.app.server.service;

public class AuctionManager {

    private static volatile AuctionManager god = null;

    private AuctionManager() {

    }

    public static AuctionManager getInstance() {
        if (god == null) {
            synchronized (AuctionManager.class) {
                if (god == null) {
                    god = new AuctionManager();
                }
            }
        }
        return god;
    }

    public void createAuction() {

    }

    public static void main(String[] args) {

    }

}
