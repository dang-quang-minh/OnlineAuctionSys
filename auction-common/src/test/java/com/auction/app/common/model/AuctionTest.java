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

class AuctionTest {

    private Auction auction;

    @BeforeEach
    void setUp() {
        Product product = new Art("P001", "Laptop", 1_000_000, "Asus");
        Seller seller = new Seller(
                "S001",
                "NguyenVanA",
                "NVA001",
                "nguyenVanA01@gmail.com"
        );
        Instant endsAt = Instant.now().plusSeconds(10 * 60);

        auction = new Auction("AUC001", product, seller, endsAt);
    }

    @Test
    void shouldStartWithOpenStatus() {
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
    }

    @Test
    void shouldChangeStatusToRunning() {
        auction.start();

        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }

    @Test
    void shouldChangeStatusToFinished() {
        auction.start();
        auction.finish();

        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
    }

    @Test
    void shouldChangeStatusToPaidWhenFinishedWithWinningBid() throws Exception {
        auction.start();
        auction.placeBid(1_200_000, "USER001");
        auction.finish();

        auction.markPaid();

        assertEquals(AuctionStatus.PAID, auction.getStatus());
    }

    @Test
    void shouldRejectPaymentWhenFinishedWithoutWinningBid() {
        auction.start();
        auction.finish();

        assertThrows(
                IllegalStateException.class,
                () -> auction.markPaid()
        );

        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
    }

    @Test
    void shouldChangeStatusToCanceled() {
        auction.start();
        auction.finish();

        auction.cancel();

        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
    }

    @Test
    void shouldRejectBidLowerThanCurrentPrice() {
        auction.start();

        assertThrows(
                InvalidBidException.class,
                () -> auction.placeBid(900_000, "USER001")
        );

        assertEquals(1_000_000.0, auction.getCurrentPrice(), 0.0);
        assertTrue(auction.getBidHistory().isEmpty());
    }

    @Test
    void shouldRejectBidEqualToCurrentPrice() {
        auction.start();

        assertThrows(
                InvalidBidException.class,
                () -> auction.placeBid(1_000_000, "USER001")
        );

        assertEquals(1_000_000.0, auction.getCurrentPrice(), 0.0);
        assertTrue(auction.getBidHistory().isEmpty());
    }

    @Test
    void shouldAcceptValidBidAndUpdatePriceAndHistory() throws Exception {
        auction.start();

        auction.placeBid(1_200_000, "USER001");

        assertEquals(1_200_000.0, auction.getCurrentPrice(), 0.0);
        assertEquals(1, auction.getBidHistory().size());
    }

    @Test
    void shouldRejectBidWhileAuctionIsOpen() {
        assertThrows(
                AuctionClosedException.class,
                () -> auction.placeBid(1_200_000, "USER001")
        );

        assertEquals(1_000_000.0, auction.getCurrentPrice(), 0.0);
        assertTrue(auction.getBidHistory().isEmpty());
    }

    @Test
    void shouldRejectBidAfterAuctionFinishes() {
        auction.start();
        auction.finish();

        assertThrows(
                AuctionClosedException.class,
                () -> auction.placeBid(1_500_000, "USER001")
        );

        assertEquals(1_000_000.0, auction.getCurrentPrice(), 0.0);
        assertTrue(auction.getBidHistory().isEmpty());
    }

    @Test
    void shouldReturnHighestBidAsWinner() throws Exception {
        auction.start();
        auction.placeBid(1_200_000, "USER001");
        auction.placeBid(1_300_000, "USER002");
        auction.placeBid(1_700_000, "USER003");

        Optional<Bid> winningBid = auction.getWinningBid();

        assertTrue(winningBid.isPresent());
        assertEquals("USER003", winningBid.orElseThrow().getBidderId());
        assertEquals(1_700_000.0, winningBid.orElseThrow().getAmount(), 0.0);
    }

    @Test
    void shouldReturnEmptyWhenThereAreNoBids() {
        assertTrue(auction.getWinningBid().isEmpty());
    }

    @Test
    void shouldRejectStartingAuctionTwice() {
        auction.start();

        assertThrows(
                IllegalStateException.class,
                () -> auction.start()
        );

        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }

    @Test
    void shouldRejectFinishingAuctionBeforeItStarts() {
        assertThrows(
                IllegalStateException.class,
                () -> auction.finish()
        );

        assertEquals(AuctionStatus.OPEN, auction.getStatus());
    }

    @Test
    void shouldRejectCancelingAuctionBeforeItFinishes() {
        auction.start();

        assertThrows(
                IllegalStateException.class,
                () -> auction.cancel()
        );

        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }

    @Test
    void shouldRejectPaymentBeforeAuctionFinishes() {
        auction.start();

        assertThrows(
                IllegalStateException.class,
                () -> auction.markPaid()
        );

        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }
}