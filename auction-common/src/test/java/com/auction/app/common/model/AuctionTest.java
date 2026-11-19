package com.auction.app.common.model;

import com.auction.app.common.exception.AuctionClosedException;
import com.auction.app.common.exception.InvalidBidException;
import com.auction.app.common.model.product.Art;
import com.auction.app.common.model.product.Product;
import com.auction.app.common.model.user.Seller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionTest {
    private Auction auction;

    @BeforeEach
    void setUp() {
        Product product = new Art("P001","Laptop",1000000,"Asus");
        Seller seller = new Seller("S001","NguyenVanA","NVA001","nguyenVanA01@gmail.com");
        Instant endsAt = Instant.now().plusSeconds(10 * 60);
        auction = new Auction(
                "AUC001",
                product,
                seller,
                endsAt);
    }

    //Test trạng thái bắt đầu
    @Test
    void shouldStartWithOpenStatus() {
        assertEquals(
                AuctionStatus.OPEN,
                auction.getStatus()
        );
    }

    //Test chuyển trạng thái
    @Test
    void shouldChangeStatusToRunning() {
        auction.start();

        assertEquals(
                AuctionStatus.RUNNING,
                auction.getStatus()
        );
    }

    @Test
    void shouldChangeStatusToFinished() {
        auction.start();
        auction.finish();

        assertEquals(
                AuctionStatus.FINISHED,
                auction.getStatus()
        );
    }

    @Test
    void shouldChangeStatusToPaid() {
        auction.start();
        try {
            auction.placeBid(1200000,"BID001");
        } catch (AuctionClosedException | InvalidBidException e) {
            throw new RuntimeException(e);
        }
        auction.finish();
        auction.markPaid();

        assertEquals(
                AuctionStatus.PAID,
                auction.getStatus()
        );
    }

    @Test
    void shouldChangeStatusToCancel() {
        auction.start();
        auction.finish();
        auction.cancel();

        assertEquals(
                AuctionStatus.CANCELED,
                auction.getStatus()
        );
    }

    //Test đặt giá không hợp lệ
    @Test
    void shouldRejectBidLowerThanCurrentPrice() {
        auction.start();

        assertThrows(
                InvalidBidException.class,
                () -> auction.placeBid(900000,"USER001")
        );
    }

    @Test
    void shouldRejectBidEqualToCurrentPrice() {
        auction.start();

        assertThrows(
                InvalidBidException.class,
                () -> auction.placeBid(1000000,"USER001")
        );
    }

    //Test bid hợp lệ
    @Test
    public void shouldAcceptValidBid() throws Exception {
        auction.start();

        auction.placeBid(1200000,"USER001");

        assertEquals(
                1200000,
                auction.getCurrentPrice()
        );

        assertEquals(
                1,
                auction.getBidHistory().size()
        );
    }

    //Test không nhận bid sau khi kết thúc
    @Test
    public void shouldRejectBidAfterAuctionFinished() {
        auction.start();
        auction.finish();

        assertThrows(
                AuctionClosedException.class,
                () -> auction.placeBid(1500000,"USER001")
        );
    }

    //Test xác định người chiến thắng
    @Test
    public void shouldReturnHighestBidAsWinner() throws Exception {
        auction.start();

        auction.placeBid(1200000,"USER001");
        auction.placeBid(1300000,"USER002");
        auction.placeBid(1700000,"USER003");

        Optional<Bid> winner = auction.getWinningBid();

        assertTrue(winner.isPresent());
        Bid winningBid = winner.orElseThrow();

        assertEquals(
                "USER003",
                winningBid.getBidderId()
        );
        assertEquals(
                1700000,
                winningBid.getAmount()
        );
    }

    // Không có người đặt giá
    @Test
    public void shouldReturnEmptyWhenNoBids() {
        assertTrue(auction.getWinningBid().isEmpty());
    }

    // Không thể bắt đầu phiên đấu giá 2 lần
    @Test
    public void shouldRejectStartingAuctionTwice() {
        auction.start();

        assertThrows(
                IllegalStateException.class,
                () -> auction.start()
        );
    }

    // Không thể thanh toán khi phiên chưa kết thúc
    @Test
    public void shouldRejectPaymentBeforeFinished() {
        auction.start();

        assertThrows(
                IllegalStateException.class,
                () -> auction.markPaid()
        );
    }
}
