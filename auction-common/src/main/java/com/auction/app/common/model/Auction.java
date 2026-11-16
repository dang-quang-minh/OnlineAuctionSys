package com.auction.app.common.model;

import com.auction.app.common.exception.AuctionClosedException;
import com.auction.app.common.exception.InvalidBidException;
import com.auction.app.common.model.product.Product;
import com.auction.app.common.model.user.Seller;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/** Một phiên đấu giá cho một sản phẩm. */
public class Auction {

    private final String auctionId;
    private final Product product;
    private final Seller seller;
    private final ReentrantLock lock = new ReentrantLock();
    private final List<Bid> bids = new ArrayList<>();
    private double currentPrice;
    private AuctionStatus status = AuctionStatus.OPEN;

    public Auction(String auctionId, Product product, Seller seller) {
        this.auctionId = Objects.requireNonNull(auctionId, "auctionId");
        this.product = Objects.requireNonNull(product, "product");
        this.seller = Objects.requireNonNull(seller, "seller");
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

    public List<Bid> getBids() {
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

    public void pay() {
        lock.lock();
        try {
            if (status != AuctionStatus.FINISHED) {
                throw new IllegalStateException("Chỉ phiên FINISHED mới có thể kết thúc.");
            }
            status = AuctionStatus.PAID;
        } finally {
            lock.unlock();
        }
    }

    public void cancel() {
        lock.lock();
        try {
            if (status != AuctionStatus.OPEN && status != AuctionStatus.RUNNING) {
                throw new IllegalStateException("Chỉ phiên OPEN hoặc RUNNING mới có thể bị hủy.");
            }
            status = AuctionStatus.CANCELED;
        }
        finally {
            lock.unlock();
        }
    }
    public void placeBid(double amount, String bidderId) throws AuctionClosedException, InvalidBidException {
        Objects.requireNonNull(bidderId, "bidderId");
        lock.lock();
        try {
            if (status != AuctionStatus.RUNNING) {
                throw new AuctionClosedException(
                        auctionId,
                        status.name()
                );
            }
            if (!Double.isFinite(amount) || amount <= currentPrice) {
                throw new InvalidBidException(currentPrice, amount);
            }
            bids.add(new Bid(bidderId, amount, LocalDateTime.now()));
            currentPrice = amount;
        } finally {
            lock.unlock();
        }
    }

    public Bid getWinningBid() {
        lock.lock();
        try {
            if (bids.isEmpty()) {
                return null;
            }

            return bids.get(bids.size() - 1);
        } finally {
            lock.unlock();
        }
    }
}
