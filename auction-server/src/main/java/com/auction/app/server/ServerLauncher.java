package com.auction.app.server;

import com.auction.app.common.dto.NetworkMessage;
import com.auction.app.common.model.Auction;
import com.auction.app.common.model.product.Art;
import com.auction.app.common.model.user.Seller;
import com.auction.app.server.network.AccountHandler;
import com.auction.app.server.network.ServerNetwork;
import com.auction.app.server.repository.AuctionDataSnapshot;
import com.auction.app.server.repository.FileStorageRepository;
import com.auction.app.server.repository.StorageRepository;
import com.auction.app.server.service.AccountService;
import com.auction.app.server.service.AuctionManager;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;

/**
 * Điểm vào để chạy server Socket độc lập trên máy local.
 */
public final class ServerLauncher {

    public static final String DEFAULT_SNAPSHOT_PATH = "data/auction_snapshot.dat";
    private static final System.Logger LOGGER = System.getLogger(ServerLauncher.class.getName());
    private static final StorageRepository STORAGE_REPOSITORY = new FileStorageRepository();

    private static volatile AccountService accountService;

    private ServerLauncher() {
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        int port = args.length == 0 ? 5050 : Integer.parseInt(args[0]);

        accountService = new AccountService(STORAGE_REPOSITORY, DEFAULT_SNAPSHOT_PATH);
        loadSnapshotOrSeed();

        AccountHandler accountHandler = new AccountHandler(accountService);
        ServerNetwork server = new ServerNetwork(port, AuctionManager.getInstance(), accountHandler);

        // Lưu snapshot khi có giao dịch phát sinh (giá mới được chấp nhận)
        AuctionManager.getInstance().registerObserver(message -> {
            if (message != null && NetworkMessage.NEW_BID.equals(message.getType())) {
                saveCurrentSnapshot();
            }
        });

        // Lưu snapshot khi tắt server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            saveCurrentSnapshot();
            server.close();
        }));

        server.start();
        System.out.println("Auction server listening on port " + server.getPort());
        new CountDownLatch(1).await();
    }

    /**
     * Lưu trạng thái dữ liệu toàn hệ thống xuống file snapshot bền vững.
     */
    public static synchronized void saveCurrentSnapshot() {
        try {
            AuctionDataSnapshot snapshot = new AuctionDataSnapshot(
                    accountService != null ? accountService.getAllUsers() : null,
                    null,
                    AuctionManager.getInstance().getAllAuctions());
            STORAGE_REPOSITORY.save(DEFAULT_SNAPSHOT_PATH, snapshot);
            LOGGER.log(System.Logger.Level.INFO, "Đã lưu snapshot dữ liệu hệ thống.");
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Không thể lưu snapshot: " + e.getMessage());
        }
    }

    private static void loadSnapshotOrSeed() {
        File file = new File(DEFAULT_SNAPSHOT_PATH);
        if (file.exists() && file.length() > 0) {
            try {
                AuctionDataSnapshot snapshot = STORAGE_REPOSITORY.load(DEFAULT_SNAPSHOT_PATH);
                if (snapshot != null) {
                    if (snapshot.getUsers() != null) {
                        snapshot.getUsers().values().forEach(AuctionManager.getInstance()::registerUser);
                    }
                    if (snapshot.getAuctions() != null && !snapshot.getAuctions().isEmpty()) {
                        snapshot.getAuctions().values().forEach(auction -> {
                            try {
                                AuctionManager.getInstance().createAuction(auction);
                            } catch (Exception ignored) {
                            }
                        });
                        LOGGER.log(System.Logger.Level.INFO, "Đã nạp snapshot từ file: " + DEFAULT_SNAPSHOT_PATH);
                        return;
                    }
                }
            } catch (Exception e) {
                LOGGER.log(System.Logger.Level.WARNING, "Không thể nạp snapshot, chuyển sang dữ liệu mẫu.", e);
            }
        }
        seedDemoAuction();
    }

    private static void seedDemoAuction() {
        AuctionManager manager = AuctionManager.getInstance();
        Seller seller = new Seller(
                "seller-demo", "seller", "demo", "seller@example.com");
        manager.registerUser(seller);
        Auction auction = new Auction(
                "auction-demo",
                new Art("product-demo", "seller-01", "Tranh phong cảnh", 1_000_000, "Nghệ sĩ demo"),
                seller,
                Instant.now().plusSeconds(3600));
        manager.createAuction(auction);
        auction.start();
    }
}
