package com.auction.app.common.model;

import com.auction.app.common.exception.AuctionClosedException;
import com.auction.app.common.exception.InvalidBidException;
import com.auction.app.common.model.product.Art;
import com.auction.app.common.model.product.Product;
import com.auction.app.common.model.user.Seller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionTest {
    private Auction auction;

    @BeforeEach
    void setUp() {
        Product product = new Art("P001","Laptop",1000000,"Asus");
        Seller seller = new Seller("S001","NguyenVanA","NVA001","nguyenVanA01@gmail.com");
        auction = new Auction(
                "AUC001",
                product,
                seller);
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
        auction.finish();
        auction.pay();

        assertEquals(
                AuctionStatus.PAID,
                auction.getStatus()
        );
    }

    @Test
    void shouldChangeStatusToCancel() {
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
                auction.getBids().size()
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

        Bid winner = auction.getWinningBid();

        assertNotNull(winner);

        assertEquals(
                "USER003",
                winner.getBidderId()
        );

        assertEquals(
                1700000,
                winner.getAmount()
        );
    }
}
