package com.auction.app.common.model;

import com.auction.app.common.exception.AuctionClosedException;
import com.auction.app.common.exception.InvalidBidException;
import com.auction.app.common.model.product.Product;
import com.auction.app.common.model.user.Seller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Một phiên đấu giá cho một sản phẩm.
 */
public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;

    // ID của phiên (immutable)
    private final String auctionId;

    // Sản phẩm cần đấu giá (immutable)
    private final Product product;

    // Người bán (immutable)
    private final Seller seller;

    // Thời điểm đóng phiên (immutable)
    private final Instant endsAt;

    // Khóa giúp chống race condition và self-deadlock ở một số
    // thuộc tính và phương thức
    private transient ReentrantLock lock = new ReentrantLock();

    // Danh sách các lượt đặt giá của phiên (immutable)
    private final List<Bid> bids = new ArrayList<>();

    // Giá đặt cao nhất của phiên (mutable, real-time)
    private double currentPrice;
    private AuctionStatus status = AuctionStatus.OPEN;

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.lock = new ReentrantLock();
    }

    // Constructor nhập thông tin chung cho phiên đấu giá
    // và thời điểm kết thúc phiên, đồng thời kiểm tra ngoại lệ phát sinh
    public Auction(String auctionId,
                   Product product,
                   Seller seller,
                   Instant endsAt) {

        // Nếu đầu vào là null thì ném NullPointerException cùng message tương ứng
        this.auctionId = Objects.requireNonNull(auctionId, "auctionId");
        this.product = Objects.requireNonNull(product, "product");
        this.seller = Objects.requireNonNull(seller, "seller");
        this.endsAt = Objects.requireNonNull(endsAt, "endsAt");

        // Nếu thời gian không hợp lệ thì ném ra ngoại lệ tương ứng
        if (!this.endsAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                    "Thời gian kết thúc phải ở tương lai."
            );
        }

        // Ném ra ngoại lệ với trường hợp tương ứng
        if (!Double.isFinite(product.getStartingPrice()) || product.getStartingPrice() < 0) {
            throw new IllegalArgumentException("Giá khởi điểm phải là số hữu hạn và không âm.");
        }

        this.currentPrice = product.getStartingPrice();
    }

    // Các phương thức lấy các thuộc tính immutable
    // nên không cần cài lock.lock()

    public String getAuctionId() {
        return auctionId;
    }

    public Product getProduct() {
        return product;
    }

    public Seller getSeller() {
        return seller;
    }

    // Phương thức lấy ra mức giá hiện tại
    public double getCurrentPrice() {
        lock.lock();
        try {
            return currentPrice;
        } finally {
            lock.unlock();
        }
    }

    // Phương thức lấy ra trạng thái hiện tai của phiên đấu giá
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

    // Phương thức lấy ra lịch sử đặt giá ở hiện tại
    public List<Bid> getBidHistory() {
        lock.lock();
        try {
            return List.copyOf(bids);
        } finally {
            lock.unlock();
        }
    }

    // Phương thức mở phiên đấu giá
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

    // Phương thức đóng phiên đấu giá
    // (cần xem lại luồng chương trình)
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

    // Phương thức giúp thanh toán cho phiên
    // (cần xem lại luồng chương trình)
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

    // Phương thức giúp hủy phiên đấu giá
    // (cần xem lại luồng chương trình)
    public void cancel() {
        lock.lock();
        try {
            if (status != AuctionStatus.FINISHED) {
                throw new IllegalStateException("Chỉ phiên FINISHED mới có thể bị hủy.");
            }
            status = AuctionStatus.CANCELED;
        } finally {
            lock.unlock();
        }
    }

    // Phương thức giúp đặt giá cho phiên
    public Bid placeBid(double amount, String bidderId)
            throws AuctionClosedException, InvalidBidException {
        // Kiểm tra sự tồn tại của ID của người đặt giá
        // và ném NullPointerException (bidderId không được null) hoặc IllegalArgumentException tùy điều kiện
        Objects.requireNonNull(bidderId, "bidderId");
        if (bidderId.isBlank()) {
            throw new IllegalArgumentException("bidderId không được để trống.");
        }
        // Khóa phương thức khi bắt đầu xét duyệt lần đặt giá
        lock.lock();
        try {

            // ném ra AuctionClosedException nếu phiên đấu giá đã đóng hoặc đã hết hạn
            if (status != AuctionStatus.RUNNING) {
                throw new AuctionClosedException(
                        auctionId,
                        status.name()
                );
            }
            if (!Instant.now().isBefore(endsAt)) {
                throw new AuctionClosedException(auctionId, "EXPIRED");
            }

            // ném ra InvalidBidException nếu mức giá không hợp lệ hoặc không đánh bại được
            // mức giá cũ
            if (!Double.isFinite(amount) || amount <= 0) {
                throw new InvalidBidException("Giá đặt phải là số hữu hạn và lớn hơn 0.");
            }
            if (amount <= currentPrice) {
                throw new InvalidBidException(currentPrice, amount);
            }
            // Lần đặt giá đủ điều kiện sẽ được lưu vào danh sách các lần đặt giá
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

    // Phương thức lấy ra lần đặt giá thắng cuộc bằng cách lấy max của bids
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
