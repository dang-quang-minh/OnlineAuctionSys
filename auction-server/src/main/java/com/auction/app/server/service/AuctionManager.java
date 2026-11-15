package com.auction.app.server.service;

public class AuctionManager {

    private static volatile AuctionManager instance = null;

    private AuctionManager() {

    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    public void createAuction() {

    }

    public static void main(String[] args) {

    }

}
