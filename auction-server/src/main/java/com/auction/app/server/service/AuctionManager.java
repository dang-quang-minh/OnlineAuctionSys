package com.auction.app.server.service;

import com.auction.app.common.model.Auction;
import com.auction.app.common.model.AuctionStatus;
import com.auction.app.common.model.user.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AuctionManager {

    private static volatile AuctionManager instance = null;
    private final Map<String, Auction> auctions = new ConcurrentHashMap<>();
    private final Map<String, User> users = new ConcurrentHashMap<>();

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

    public void registerUser(User user) {
        if (user != null) {
            users.put(user.getId(), user);
        }
    }

    public User getUserById(String id) {
        if (users.containsKey(id)) {
            return users.get(id);
        }
        return null;
    }

    public void createAuction(Auction auction) {
        if (auction != null) {
            auctions.put(auction.getAuctionId(), auction);
        }
    }
    public Auction getAuction(String id) {
        if (auctions.containsKey(id)) {
            return auctions.get(id);
        }
        return null;
    }
    public ConcurrentHashMap<String, Auction> getAllAuctions() {
        return new ConcurrentHashMap<>(auctions);
    }
    public ConcurrentHashMap<String, Auction> getActiveAuctions() {
        ConcurrentHashMap<String, Auction> activeAuctions = new ConcurrentHashMap<>();
        for (Auction entry : auctions.values()) {
            if (entry.getStatus().equals(AuctionStatus.OPEN)) {
                activeAuctions.put(entry.getAuctionId(), entry);
            }
        }
        return activeAuctions;
    }


}
