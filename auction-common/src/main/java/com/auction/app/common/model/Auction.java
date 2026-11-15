package com.auction.app.common.model;

import com.auction.app.common.exception.AuctionClosedException;
import com.auction.app.common.exception.InvalidBidException;
import com.auction.app.common.model.product.Product;
import com.auction.app.common.model.user.Seller;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String auctionId;
    private final Product product;
    private final Seller seller;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final ReentrantLock lock = new ReentrantLock();
    private final List<Bid> bidHistory = new ArrayList<>();

    private volatile AuctionStatus status = AuctionStatus.OPEN;
    private volatile Bid highestBid;

    public Auction(String auctionId, Product product, Seller seller, LocalDateTime startTime, LocalDateTime endTime) {
        this.auctionId = Objects.requireNonNull(auctionId, "auctionId");
        this.product = Objects.requireNonNull(product, "product");
        this.seller = Objects.requireNonNull(seller, "seller");
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        this.endTime = Objects.requireNonNull(endTime, "endTime");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("The end time must be after the start time");
        }
        if (!Double.isFinite(product.getStartingPrice()) || product.getStartingPrice() < 0) {
            throw new IllegalArgumentException("The starting price must be finite and nonnegative");
        }
        refreshStatus(LocalDateTime.now());
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

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public Bid getHighestBid() {
        return highestBid;
    }

    public double getCurrentPrice() {
        Bid bid = highestBid;
        return bid == null ? product.getStartingPrice() : bid.getAmount();
    }

    public List<Bid> getBidHistory() {
        lock.lock();
        try {
            return List.copyOf(bidHistory);
        } finally {
            lock.unlock();
        }
    }

    public void refreshStatus(LocalDateTime now) {
        Objects.requireNonNull(now, "now");
        lock.lock();
        try {
            if (status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING) {
                if (!now.isBefore(endTime)) {
                    status = AuctionStatus.FINISHED;
                } else if (!now.isBefore(startTime)) {
                    status = AuctionStatus.RUNNING;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void placeBid(double amount, String bidderId) throws AuctionClosedException, InvalidBidException {
        Objects.requireNonNull(bidderId, "bidderId");
        lock.lock();
        try {
            LocalDateTime now = LocalDateTime.now();
            refreshStatus(now);
            if (status != AuctionStatus.RUNNING) {
                throw new AuctionClosedException(auctionId, status.name());
            }
            if (!Double.isFinite(amount) || amount <= getCurrentPrice()) {
                throw new InvalidBidException(getCurrentPrice(), amount);
            }
            Bid bid = new Bid(UUID.randomUUID().toString(), bidderId, amount, now);
            bidHistory.add(bid);
            highestBid = bid;
        } finally {
            lock.unlock();
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        lock.lock();
        try {
            out.defaultWriteObject();
        } finally {
            lock.unlock();
        }
    }
}
