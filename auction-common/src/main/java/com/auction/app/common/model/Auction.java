package com.auction.app.common.model;

import com.auction.app.common.exception.AuctionClosedException;
import com.auction.app.common.exception.InvalidBidException;
import com.auction.app.common.model.product.Product;
import com.auction.app.common.model.user.Seller;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/** Một phiên đấu giá cho một sản phẩm. */
public class Auction {

    private final String auctionId
    private final Product product;
    private final Seller seller;

    private final Instant endsAt;

    private final ReentrantLock lock = new ReentrantLock();
    private final List<Bid> bids = new ArrayList<>();

    private double currentPrice;
    private AuctionStatus status = AuctionStatus.OPEN;

    public Auction(String auctionId, Product product, Seller seller, Instant endsAt) {
        this.auctionId = Objects.requireNonNull(auctionId, "auctionId");
        this.product = Objects.requireNonNull(product, "product");
        this.seller = Objects.requireNonNull(seller, "seller");
        this.endsAt = Objects.requireNonNull(endsAt, "endsAt");

        if (!this.endsAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                    "Thời gian kết thúc phải ở tương lai."
            );
        }
        if (!Double.isFinite(product.getStartingPrice()) || product.getStartingPrice() < 0) {
            throw new IllegalArgumentException("Giá khởi điểm phải là số hữu hạn và không âm.");
        }
        this.currentPrice = product.getStartingPrice();
    }

    public String getAuctionId() {
        return auctionId;
    }

    public Product getProduct() {
        return product;
    }

    public Seller getSeller() {
        return seller;
    }

    public double getCurrentPrice() {
        lock.lock();
        try {
            return currentPrice;
        } finally {
            lock.unlock();
        }
    }

    public AuctionStatus getStatus() {
        lock.lock();
        try {
            return status;
        } finally {
            lock.unlock();
        }
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public List<Bid> getBidHistory() {
        lock.lock();
        try {
            return List.copyOf(bids);
        } finally {
            lock.unlock();
        }
    }

    public void start() {
        lock.lock();
        try {
            if (status != AuctionStatus.OPEN) {
                throw new IllegalStateException("Chỉ phiên OPEN mới có thể bắt đầu.");
            }
            status = AuctionStatus.RUNNING;
        } finally {
            lock.unlock();
        }
    }

    public void finish() {
        lock.lock();
        try {
            if (status != AuctionStatus.RUNNING) {
                throw new IllegalStateException("Chỉ phiên RUNNING mới có thể kết thúc.");
            }
            status = AuctionStatus.FINISHED;
        } finally {
            lock.unlock();
        }
    }

    public void markPaid() {
        lock.lock();
        try {
            if (status != AuctionStatus.FINISHED) {
                throw new IllegalStateException(
                        "Chỉ phiên FINISHED mới có thể thanh toán."
                );
            }

            if (getBidHistory().isEmpty()) {
                throw new IllegalStateException("Phiên đấu giá không có người thắng, không thể chuyển sang PAID.");
            }
            status = AuctionStatus.PAID;
        } finally {
            lock.unlock();
        }
    }

    public void cancel() {
        lock.lock();
        try {
            if (status != AuctionStatus.FINISHED) {
                throw new IllegalStateException("Chỉ phiên FINISHED mới có thể bị hủy.");
            }
            status = AuctionStatus.CANCELED;
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Nhận đúng một bid khi phiên đang chạy, chưa hết hạn và giá lớn hơn giá hiện tại.
     *
     * @throws NullPointerException nếu bidderId là null
     * @throws IllegalArgumentException nếu bidderId rỗng hoặc chỉ có khoảng trắng
     * @throws AuctionClosedException nếu phiên không nhận bid hoặc đã tới endsAt
     * @throws InvalidBidException nếu amount không hữu hạn, không dương hoặc không tăng giá
     */
    public Bid placeBid(double amount, String bidderId) throws AuctionClosedException, InvalidBidException {
        Objects.requireNonNull(bidderId, "bidderId");
        if (bidderId.isBlank()) {
            throw new IllegalArgumentException("bidderId không được để trống.");
        }
        lock.lock();
        try {
            if (status != AuctionStatus.RUNNING) {
                throw new AuctionClosedException(
                        auctionId,
                        status.name()
                );
            }
            if (!Instant.now().isBefore(endsAt)) {
                throw new AuctionClosedException(auctionId, "EXPIRED");
            }
            if (!Double.isFinite(amount) || amount <= 0) {
                throw new InvalidBidException("Giá đặt phải là số hữu hạn và lớn hơn 0.");
            }
            if (amount <= currentPrice) {
                throw new InvalidBidException(currentPrice, amount);
            }
            Bid bid = new Bid(
                    UUID.randomUUID().toString(),
                    bidderId,
                    amount,
                    Instant.now()
            );
            bids.add(bid);
            currentPrice = amount;

            return bid;
        } finally {
            lock.unlock();
        }
    }

    public Optional<Bid> getWinningBid() {
        lock.lock();
        try {
            return bids.stream()
                    .max(Comparator.comparingDouble(Bid::getAmount));
        } finally {
            lock.unlock();
        }

    }
}
