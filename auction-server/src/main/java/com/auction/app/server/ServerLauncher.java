package com.auction.app.server;

import com.auction.app.common.model.Auction;
import com.auction.app.common.model.product.Art;
import com.auction.app.common.model.user.Seller;
import com.auction.app.server.network.ServerNetwork;
import com.auction.app.server.service.AuctionManager;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;

/**
 * Điểm vào để chạy server Socket độc lập trên máy local.
 */
public final class ServerLauncher {

    private ServerLauncher() {
    }

    public static void main(String[] args) throws InterruptedException,
            java.io.IOException {
        int port = args.length == 0 ? 5050 : Integer.parseInt(args[0]);
        seedDemoAuction();
        try (ServerNetwork server = new ServerNetwork(port)) {
            server.start();
            System.out.println("Auction server listening on port " + server.getPort());
            Runtime.getRuntime().addShutdownHook(new Thread(server::close));
            new CountDownLatch(1).await();
        }
    }

    private static void seedDemoAuction() {
        AuctionManager manager = AuctionManager.getInstance();
        Seller seller = new Seller(
                "seller-demo", "seller", "demo", "seller@example.com");
        manager.registerUser(seller);
        Auction auction = new Auction(
                "auction-demo",
                new Art("product-demo", "Tranh phong cảnh", 1_000_000, "Nghệ sĩ demo"),
                seller,
                Instant.now().plusSeconds(3600));
        manager.createAuction(auction);
        auction.start();
    }
}
