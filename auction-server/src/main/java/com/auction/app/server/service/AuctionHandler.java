package com.auction.app.server.service;

import com.auction.app.common.dto.AuctionCreateDTO;
import com.auction.app.common.dto.AuctionDTO;
import com.auction.app.common.model.Auction;
import com.auction.app.common.model.product.Product;
import com.auction.app.common.model.user.Seller;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Xử lý yêu cầu phiên sau khi tầng mạng đã xác thực người bán và tra sản phẩm.
 */
public final class AuctionHandler {
    private final AuctionManager manager;

    public AuctionHandler(AuctionManager manager) {
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    /**
     * Sản phẩm phải được tầng danh mục xác nhận thuộc về người bán trước khi gọi.
     */
    public AuctionDTO createAuction(AuctionCreateDTO request, Seller seller, Product product) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(seller, "seller");
        Objects.requireNonNull(product, "product");
        if (!product.getId().equals(request.productId())) {
            throw new IllegalArgumentException("Sản phẩm không khớp với yêu cầu.");
        }
        if (!Double.isFinite(request.startingPrice()) || request.startingPrice() < 0
                || Double.compare(request.startingPrice(), product.getStartingPrice()) != 0) {
            throw new IllegalArgumentException("Giá khởi điểm phải khớp với giá của sản phẩm.");
        }
        if (request.durationSeconds() <= 0) {
            throw new IllegalArgumentException("Thời gian đấu giá phải lớn hơn 0.");
        }
        Instant endsAt;
        try {
            endsAt = Instant.now().plusSeconds(request.durationSeconds());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Thời gian đấu giá không hợp lệ.", exception);
        }
        Auction auction = new Auction(UUID.randomUUID().toString(), product, seller, endsAt);
        auction.start();
        manager.createAuction(auction);
        return new AuctionDTO(auction.getAuctionId(), product.getName(), auction.getCurrentPrice(),
                auction.getStatus(), auction.getEndsAt());
    }

    /** Trả phiên để tầng mạng chuyển thành DTO đã thống nhất với Job 4. */
    public Auction getAuction(String auctionId) {
        return manager.getAuction(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên đấu giá."));
    }
}
