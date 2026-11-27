package com.auction.app.server.service;

import com.auction.app.common.dto.BidRequestDTO;
import com.auction.app.common.dto.NetworkMessage;
import com.auction.app.common.exception.AuctionClosedException;
import com.auction.app.common.exception.InvalidBidException;
import com.auction.app.common.model.Auction;
import com.auction.app.common.model.AuctionStatus;
import com.auction.app.common.model.Bid;
import com.auction.app.common.model.product.Art;
import com.auction.app.common.model.user.Seller;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

/**
 * Test hồi quy đi cùng phần sửa Job 2; không thay thế bộ test tổng thể của Job 3.
 */
class Week8BidErrorHandlingTest {

    @ParameterizedTest
    @ValueSource(doubles = {-1, -0.0, 0, Double.NaN, Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY, 149, 150})
    void invalidAmountsKeepModelAndEventsUnchanged(double amount) throws Exception {
        try (Scenario scenario = new Scenario()) {
            scenario.startWithAcceptedBid();
            Snapshot before = scenario.snapshot();

            InvalidBidException direct = assertThrows(InvalidBidException.class,
                    () -> scenario.auction.placeBid(amount, "U2"));
            assertEquals("INVALID_BID", direct.getErrorCode());
            InvalidBidException throughManager = assertThrows(InvalidBidException.class,
                    () -> scenario.manager.processBid("A1", new BidRequestDTO("A1", "U2", amount)));
            assertEquals("INVALID_BID", throughManager.getErrorCode());
            if (!Double.isFinite(amount)) {
                assertFalse(throughManager.getMessage().contains("NaN"));
                assertFalse(throughManager.getMessage().contains("Infinity"));
            }
            assertEquals(before, scenario.snapshot());
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void missingBidderIdIsAProgrammingErrorWithoutMutation(String bidderId) throws Exception {
        try (Scenario scenario = new Scenario()) {
            scenario.startWithAcceptedBid();
            Snapshot before = scenario.snapshot();
            Class<? extends RuntimeException> expected = bidderId == null
                    ? NullPointerException.class : IllegalArgumentException.class;

            assertThrows(expected, () -> scenario.auction.placeBid(200, bidderId));
            assertThrows(expected,
                    () -> scenario.manager.processBid("A1", new BidRequestDTO("A1", bidderId, 200)));

            assertEquals(before, scenario.snapshot());
        }
    }

    @ParameterizedTest
    @EnumSource(value = AuctionStatus.class, names = {"OPEN", "FINISHED", "PAID", "CANCELED"})
    void nonRunningAuctionsRejectBidsWithoutChangingStateOrEvents(AuctionStatus status) throws Exception {
        try (Scenario scenario = new Scenario()) {
            if (status != AuctionStatus.OPEN) {
                scenario.startWithAcceptedBid();
                scenario.auction.finish();
                if (status == AuctionStatus.PAID) {
                    scenario.auction.markPaid();
                } else if (status == AuctionStatus.CANCELED) {
                    scenario.auction.cancel();
                }
            }
            Snapshot before = scenario.snapshot();

            AuctionClosedException failure = assertThrows(AuctionClosedException.class,
                    () -> scenario.manager.processBid("A1", new BidRequestDTO("A1", "U2", 200)));

            assertEquals("AUCTION_CLOSED", failure.getErrorCode());
            assertEquals(before, scenario.snapshot());
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "missing"})
    void invalidAuctionIdDoesNotChangeTheExistingAuction(String auctionId) throws Exception {
        try (Scenario scenario = new Scenario()) {
            scenario.startWithAcceptedBid();
            Snapshot before = scenario.snapshot();

            assertThrows(IllegalArgumentException.class,
                    () -> scenario.manager.processBid(auctionId, new BidRequestDTO(auctionId, "U2", 200)));

            assertEquals(before, scenario.snapshot());
        }
    }

    @Test
    void nullRequestAndMismatchedRequestIdKeepDataAndEventsUnchanged() throws Exception {
        try (Scenario scenario = new Scenario()) {
            scenario.startWithAcceptedBid();
            Snapshot before = scenario.snapshot();

            assertThrows(NullPointerException.class, () -> scenario.manager.processBid("A1", null));
            for (String requestId : new String[]{null, "", " ", "other"}) {
                assertThrows(IllegalArgumentException.class,
                        () -> scenario.manager.processBid("A1", new BidRequestDTO(requestId, "U2", 200)));
            }
            assertEquals(before, scenario.snapshot());
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 1})
    void deadlineIsExclusiveWithoutSleepingOrRelyingOnScheduler(long offsetNanos) throws Exception {
        Instant now = Instant.parse("2026-11-19T00:00:00Z");
        Instant endsAt = now.plusSeconds(60);
        // Only the calling thread's Instant.now() is controlled; model/DTO/event code is real.
        try (MockedStatic<Instant> time = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            time.when(Instant::now).thenReturn(now);
            try (Scenario scenario = new Scenario(Clock.fixed(now, ZoneOffset.UTC), endsAt)) {
                scenario.startWithAcceptedBid();
                Snapshot before = scenario.snapshot();
                Instant bidTime = endsAt.plusNanos(offsetNanos);
                time.when(Instant::now).thenReturn(bidTime);

                if (offsetNanos < 0) {
                    scenario.manager.processBid("A1", new BidRequestDTO("A1", "U2", 200));
                    assertEquals(200, scenario.auction.getCurrentPrice());
                    assertEquals(2, scenario.auction.getBidHistory().size());
                    assertEquals(2, scenario.events.size());
                    assertTrue(scenario.auction.getWinningBid().orElseThrow().getBidTime().isBefore(endsAt));
                } else {
                    AuctionClosedException failure = assertThrows(AuctionClosedException.class,
                            () -> scenario.manager.processBid("A1", new BidRequestDTO("A1", "U2", 200)));
                    assertEquals("AUCTION_CLOSED", failure.getErrorCode());
                    assertEquals(before, scenario.snapshot());
                }
            }
        }
    }

    private record Snapshot(double price, List<Bid> history, Optional<Bid> winner,
                            AuctionStatus status, List<NetworkMessage> events) {
    }

    private static final class Scenario implements AutoCloseable {
        private final AuctionManager manager;
        private final Auction auction;
        private final List<NetworkMessage> events = new ArrayList<>();

        private Scenario() {
            this(Clock.systemUTC(), Instant.now().plusSeconds(120));
        }

        private Scenario(Clock clock, Instant endsAt) {
            auction = new Auction("A1", new Art("P1", "S01", "Demo", 100, "Artist"),
                    new Seller("S1", "seller", "demo", "seller@example.com"), endsAt);
            manager = new AuctionManager(Executors.newSingleThreadScheduledExecutor(), clock);
            manager.registerObserver(events::add);
            manager.createAuction(auction);
        }

        private void startWithAcceptedBid() throws Exception {
            auction.start();
            manager.processBid("A1", new BidRequestDTO("A1", "U1", 150));
            assertEquals(1, events.size());
            assertEquals(NetworkMessage.NEW_BID, events.get(0).getType());
        }

        private Snapshot snapshot() {
            return new Snapshot(auction.getCurrentPrice(), auction.getBidHistory(), auction.getWinningBid(),
                    auction.getStatus(), List.copyOf(events));
        }

        @Override
        public void close() {
            manager.close();
        }
    }
}
