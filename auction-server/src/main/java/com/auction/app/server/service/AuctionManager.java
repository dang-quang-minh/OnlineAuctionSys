package com.auction.app.server.service;

import com.auction.app.common.dto.BidDTO;
import com.auction.app.common.dto.BidRequestDTO;
import com.auction.app.common.dto.AuctionEndDTO;
import com.auction.app.common.dto.NetworkMessage;
import com.auction.app.common.exception.AuctionClosedException;
import com.auction.app.common.exception.InvalidBidException;
import com.auction.app.common.model.Auction;
import com.auction.app.common.model.AuctionStatus;
import com.auction.app.common.model.Bid;
import com.auction.app.common.model.user.User;
import com.auction.app.common.observer.Observer;
import com.auction.app.common.observer.Subject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Điều phối phiên đấu giá; luật đặt giá và khóa thuộc về {@link Auction}.
 * Gọi {@link #close()} khi dừng server để hủy các lịch đóng phiên còn lại.
 */
public final class AuctionManager implements Subject, AutoCloseable {

    private static final System.Logger LOGGER = System.getLogger(AuctionManager.class.getName());
    private static final long OPEN_RECHECK_DELAY_NANOS = TimeUnit.MILLISECONDS.toNanos(100);

    private final ConcurrentMap<String, Auction> auctions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, User> users = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Observer> observers = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler;
    private final Clock clock;
    private final Object lifecycleLock = new Object();
    private volatile boolean closed;

    private AuctionManager() {
        this(Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "auction-deadlines");
            thread.setDaemon(true);
            return thread;
        }), Clock.systemUTC());
    }

    // Cho test dùng clock/scheduler xác định, không phải chờ thời gian thực.
    AuctionManager(ScheduledExecutorService scheduler, Clock clock) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    private static final class InstanceHolder {
        private static final AuctionManager INSTANCE = new AuctionManager();
    }

    public static AuctionManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    // Giữ API quản lý user hiện có; Job 2 không triển khai đăng nhập.
    public void registerUser(User user) {
        if (user != null) {
            users.put(user.getId(), user);
        }
    }

    public User getUserById(String id) {
        return users.get(Objects.requireNonNull(id, "id"));
    }

    /** Đăng ký phiên và lịch đóng; không ghi đè phiên có cùng ID. */
    public Auction createAuction(Auction auction) {
        Objects.requireNonNull(auction, "auction");
        String auctionId = requireAuctionId(auction.getAuctionId());
        Instant endsAt = Objects.requireNonNull(auction.getEndsAt(), "auction.endsAt");

        synchronized (lifecycleLock) {
            ensureOpen();
            if (auctions.putIfAbsent(auctionId, auction) != null) {
                throw new IllegalArgumentException("Phiên đấu giá đã tồn tại: " + auctionId);
            }
            try {
                scheduler.schedule(() -> finishAuction(auction), delayUntil(endsAt), TimeUnit.NANOSECONDS);
            } catch (RuntimeException exception) {
                // Không để lại một phiên không có lịch đóng nếu scheduler từ chối.
                auctions.remove(auctionId, auction);
                throw exception;
            }
        }
        return auction;
    }

    public Optional<Auction> getAuction(String auctionId) {
        return Optional.ofNullable(auctions.get(requireAuctionId(auctionId)));
    }

    /** Snapshot độc lập với map nội bộ, giữ kiểu trả về của API cũ. */
    public ConcurrentHashMap<String, Auction> getAllAuctions() {
        return new ConcurrentHashMap<>(auctions);
    }

    /** Snapshot không sửa được; chỉ lọc OPEN/RUNNING, không sao chép model. */
    public Map<String, Auction> getActiveAuctions() {
        Map<String, Auction> activeAuctions = new HashMap<>();
        auctions.forEach((auctionId, auction) -> {
            AuctionStatus status = auction.getStatus();
            if (status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING) {
                activeAuctions.put(auctionId, auction);
            }
        });
        return Map.copyOf(activeAuctions);
    }

    /**
     * Một lần gọi model, một thông báo NEW_BID khi thành công.
     * Không đọc bid cuối từ lịch sử vì bid khác có thể được nhận đồng thời.
     * Lỗi từ model được truyền nguyên vẹn cho bên gọi, không phát event khi bid bị từ chối.
     *
     * @throws InvalidBidException nếu giá không hợp lệ
     * @throws AuctionClosedException nếu phiên không nhận bid hoặc đã tới hạn
     * @throws IllegalArgumentException nếu ID rỗng, không tồn tại, không khớp hoặc bidderId rỗng
     * @throws NullPointerException nếu request hoặc bidderId là null
     * @throws IllegalStateException nếu Manager đã dừng
     */
    public BidDTO processBid(String auctionId, BidRequestDTO request)
            throws InvalidBidException, AuctionClosedException {
        ensureOpen();
        requireAuctionId(auctionId);
        Objects.requireNonNull(request, "request");
        if (!auctionId.equals(request.auctionId())) {
            throw new IllegalArgumentException("auctionId không khớp với yêu cầu đặt giá.");
        }
        Auction auction = getAuction(auctionId).orElseThrow(
                () -> new IllegalArgumentException("Không tìm thấy phiên đấu giá: " + auctionId));

        Bid bid = auction.placeBid(request.amount(), request.bidderId());
        BidDTO result = new BidDTO(
                bid.getBidId(), auctionId, bid.getBidderId(), bid.getAmount(), bid.getBidTime());
        notifyObservers(new NetworkMessage(NetworkMessage.NEW_BID, result));
        return result;
    }

    @Override
    public void registerObserver(Observer observer) {
        observers.addIfAbsent(Objects.requireNonNull(observer, "observer"));
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(NetworkMessage message) {
        Objects.requireNonNull(message, "message");
        for (Observer observer : observers) {
            try {
                observer.update(message);
            } catch (RuntimeException exception) {
                // Bid đã được nhận; một observer lỗi không được biến nó thành bid thất bại.
                LOGGER.log(System.Logger.Level.WARNING, "Không gửi được thông báo tới observer.", exception);
            }
        }
    }

    private long delayUntil(Instant endsAt) {
        Duration delay = Duration.between(clock.instant(), endsAt);
        if (delay.isNegative() || delay.isZero()) {
            return 0;
        }
        try {
            return delay.toNanos();
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    private void finishAuction(Auction auction) {
        if (closed) {
            return;
        }
        AuctionStatus status = auction.getStatus();
        if (status == AuctionStatus.OPEN) {
            synchronized (lifecycleLock) {
                // Phiên có thể được start trễ. Không bỏ mất lịch, không tự ép OPEN -> FINISHED.
                if (!closed) {
                    scheduler.schedule(() -> finishAuction(auction), OPEN_RECHECK_DELAY_NANOS, TimeUnit.NANOSECONDS);
                }
            }
            return;
        }
        if (status != AuctionStatus.RUNNING) {
            return;
        }
        try {
            // Auction.finish() dùng cùng khóa với placeBid(), manager không lấy khóa ra ngoài.
            auction.finish();
            Optional<Bid> winner = auction.getWinningBid();
            AuctionEndDTO result = new AuctionEndDTO(auction.getAuctionId(),
                    winner.map(Bid::getBidderId).orElse(null), auction.getCurrentPrice(), clock.instant());
            notifyObservers(new NetworkMessage(NetworkMessage.AUCTION_ENDED, result));
        } catch (IllegalStateException exception) {
            // Một luồng khác có thể đã finish giữa getStatus() và finish().
            LOGGER.log(System.Logger.Level.DEBUG, "Phiên đã đổi trạng thái trước lịch đóng.", exception);
        }
    }

    private static String requireAuctionId(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            throw new IllegalArgumentException("auctionId không được để trống.");
        }
        return auctionId;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("AuctionManager đã dừng.");
        }
    }

    @Override
    public void close() {
        synchronized (lifecycleLock) {
            if (!closed) {
                closed = true;
                scheduler.shutdownNow();
            }
        }
    }
}
