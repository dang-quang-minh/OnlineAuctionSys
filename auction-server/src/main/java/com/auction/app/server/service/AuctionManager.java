package com.auction.app.server.service;

import com.auction.app.common.exception.BaseAppException;
import com.auction.app.common.model.Auction;
import com.auction.app.common.model.AuctionStatus;
import com.auction.app.common.model.product.Product;
import com.auction.app.common.model.product.ProductFactory;
import com.auction.app.common.model.user.Seller;
import com.auction.app.common.model.user.User;

import java.time.Duration;
import java.time.Instant;
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

    public void loadDemoData() {
        if (!auctions.isEmpty()) {
            return;
        }

        Seller landscapeSeller = new Seller(
                "seller-001", "nguyen.minh.anh", "demo", "anh@example.com");
        Seller vehicleSeller = new Seller(
                "seller-002", "tran.hoang.nam", "demo", "nam@example.com");
        Seller potterySeller = new Seller(
                "seller-003", "le.thu.ha", "demo", "ha@example.com");
        registerUser(landscapeSeller);
        registerUser(vehicleSeller);
        registerUser(potterySeller);

        Product painting = ProductFactory.createProduct(
                "ART", "product-001", "Tranh sơn dầu phong cảnh", 800_000,
                Map.of("artist", "Nguyễn Hoàng"));
        Product vehicle = ProductFactory.createProduct(
                "VEHICLE", "product-002", "Xe máy cổ Honda Cub 50", 15_000_000,
                Map.of("manufacturingYear", 1982));
        Product pottery = ProductFactory.createProduct(
                "ARTIFACT", "product-003", "Bình gốm nghệ thuật Bát Tràng", 500_000,
                Map.of("origin", "Bát Tràng, Việt Nam"));

        Auction paintingAuction = new Auction(
                "AUC-001", painting, landscapeSeller, Instant.now().plus(Duration.ofHours(8)));
        Auction vehicleAuction = new Auction(
                "AUC-002", vehicle, vehicleSeller, Instant.now().plus(Duration.ofHours(10)));
        Auction potteryAuction = new Auction(
                "AUC-003", pottery, potterySeller, Instant.now().plus(Duration.ofHours(2)));

        paintingAuction.start();
        placeDemoBid(paintingAuction, 900_000, "bidder-001");
        placeDemoBid(paintingAuction, 1_200_000, "bidder-002");

        vehicleAuction.start();
        placeDemoBid(vehicleAuction, 16_500_000, "bidder-003");
        placeDemoBid(vehicleAuction, 18_500_000, "bidder-004");

        potteryAuction.start();
        placeDemoBid(potteryAuction, 650_000, "bidder-005");
        placeDemoBid(potteryAuction, 850_000, "bidder-006");
        potteryAuction.finish();

        createAuction(paintingAuction);
        createAuction(vehicleAuction);
        createAuction(potteryAuction);
    }

    private void placeDemoBid(Auction auction, double amount, String bidderId) {
        try {
            auction.placeBid(amount, bidderId);
        } catch (BaseAppException exception) {
            throw new IllegalStateException("Không thể khởi tạo dữ liệu đấu giá mẫu.", exception);
        }
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
