package com.auction.app.server.service;

import com.auction.app.common.dto.BidDTO;
import com.auction.app.common.dto.BidRequestDTO;
import com.auction.app.common.dto.NetworkMessage;
import com.auction.app.common.exception.AuctionClosedException;
import com.auction.app.common.exception.InvalidBidException;
import com.auction.app.common.model.Auction;
import com.auction.app.common.model.AuctionStatus;
import com.auction.app.common.model.Bid;
import com.auction.app.common.model.product.Art;
import com.auction.app.common.model.user.Seller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Kiểm tra Job 2 với model và hợp đồng DTO/observer thật của Job 1/3. */
class AuctionManagerIntegrationTest {

    @Test
    void acceptedBidMapsRealModelAndSurvivesMessageSerialization() throws Exception {
        try (AuctionManager manager = newManager()) {
            Auction auction = auction("A1");
            auction.start();
            manager.createAuction(auction);
            List<NetworkMessage> events = new ArrayList<>();
            manager.registerObserver(events::add);

            BidDTO result = manager.processBid("A1", new BidRequestDTO("A1", "U1", 150));

            assertEquals(1, auction.getBidHistory().size());
            Bid actualBid = auction.getBidHistory().get(0);
            assertEquals(toDto("A1", actualBid), result);
            assertEquals(150, auction.getCurrentPrice());
            assertEquals(1, events.size());
            assertEquals(NetworkMessage.NEW_BID, events.get(0).getType());
            assertSame(result, events.get(0).getPayload());

            NetworkMessage restored = roundTrip(events.get(0));
            assertEquals(NetworkMessage.NEW_BID, restored.getType());
            assertEquals(result, assertInstanceOf(BidDTO.class, restored.getPayload()));
        }
    }

    @Test
    void rejectedRequestsDoNotAddEventsOrMutateTheRealAuction() throws Exception {
        try (AuctionManager manager = newManager()) {
            Auction auction = auction("A1");
            manager.createAuction(auction);
            List<NetworkMessage> events = new ArrayList<>();
            manager.registerObserver(events::add);

            assertThrows(AuctionClosedException.class,
                    () -> manager.processBid("A1", new BidRequestDTO("A1", "U1", 150)));
            assertTrue(auction.getBidHistory().isEmpty());
            assertEquals(100, auction.getCurrentPrice());
            assertTrue(events.isEmpty());

            auction.start();
            BidDTO accepted = manager.processBid("A1", new BidRequestDTO("A1", "U1", 150));
            List<Bid> acceptedHistory = auction.getBidHistory();

            assertThrows(InvalidBidException.class,
                    () -> manager.processBid("A1", new BidRequestDTO("A1", "U2", 150)));
            assertThrows(InvalidBidException.class,
                    () -> manager.processBid("A1", new BidRequestDTO("A1", "U2", 149)));
            assertThrows(IllegalArgumentException.class,
                    () -> manager.processBid("missing", new BidRequestDTO("missing", "U2", 200)));
            assertThrows(IllegalArgumentException.class,
                    () -> manager.processBid("A1", new BidRequestDTO("other", "U2", 200)));
            assertEquals(AuctionStatus.RUNNING, auction.getStatus());

            auction.finish();
            assertThrows(AuctionClosedException.class,
                    () -> manager.processBid("A1", new BidRequestDTO("A1", "U2", 200)));

            assertEquals(AuctionStatus.FINISHED, auction.getStatus());
            assertEquals(acceptedHistory, auction.getBidHistory());
            assertEquals(150, auction.getCurrentPrice());
            assertEquals(1, events.size());
            assertSame(accepted, events.get(0).getPayload());
        }
    }

    @Test
    void concurrentRealBidsMatchHistoryEventsAndWinnerWithoutLostUpdates() throws Exception {
        try (AuctionManager manager = newManager()) {
            Auction auction = auction("A1");
            auction.start();
            manager.createAuction(auction);
            ConcurrentLinkedQueue<NetworkMessage> events = new ConcurrentLinkedQueue<>();
            ConcurrentLinkedQueue<String> rejectedBidders = new ConcurrentLinkedQueue<>();
            manager.registerObserver(events::add);
            ExecutorService workers = Executors.newFixedThreadPool(4);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Optional<BidDTO>>> results = new ArrayList<>();
            List<BidDTO> accepted = new ArrayList<>();
            try {
                for (int index = 0; index < 16; index++) {
                    String bidderId = "U" + index;
                    // Một request cố ý bằng giá khởi điểm; các request còn lại tranh giá thật.
                    double amount = index == 0 ? 100 : 200 + index;
                    results.add(workers.submit(() -> {
                        start.await();
                        try {
                            return Optional.of(manager.processBid("A1", new BidRequestDTO("A1", bidderId, amount)));
                        } catch (InvalidBidException exception) {
                            rejectedBidders.add(bidderId);
                            return Optional.empty();
                        }
                    }));
                }
                start.countDown();
                for (Future<Optional<BidDTO>> result : results) {
                    result.get(10, TimeUnit.SECONDS).ifPresent(accepted::add);
                }
            } finally {
                start.countDown();
                workers.shutdownNow();
            }

            List<Bid> history = auction.getBidHistory();
            assertFalse(rejectedBidders.isEmpty());
            assertEquals(16, accepted.size() + rejectedBidders.size());
            assertEquals(accepted.size(), history.size());

            assertEquals(accepted.size(), events.size());
            Map<String, BidDTO> acceptedById = accepted.stream()
                    .collect(Collectors.toMap(BidDTO::bidId, Function.identity()));
            Map<String, BidDTO> modelById = history.stream().map(bid -> toDto("A1", bid))
                    .collect(Collectors.toMap(BidDTO::bidId, Function.identity()));
            Map<String, BidDTO> eventsById = events.stream().map(event -> {
                assertEquals(NetworkMessage.NEW_BID, event.getType());
                return assertInstanceOf(BidDTO.class, event.getPayload());
            }).collect(Collectors.toMap(BidDTO::bidId, Function.identity()));
            assertEquals(acceptedById, modelById);
            assertEquals(acceptedById, eventsById);
            Set<String> acceptedBidders = accepted.stream().map(BidDTO::bidderId).collect(Collectors.toSet());
            assertTrue(rejectedBidders.stream().noneMatch(acceptedBidders::contains));
            assertEquals(215, auction.getCurrentPrice());
            Bid winner = auction.getWinningBid().orElseThrow();
            assertEquals("U15", winner.getBidderId());
            assertEquals(215, winner.getAmount());
            assertEquals(acceptedById.get(winner.getBidId()), toDto("A1", winner));
        }
    }

    private AuctionManager newManager() {
        return new AuctionManager(Executors.newSingleThreadScheduledExecutor(), Clock.systemUTC());
    }

    private Auction auction(String id) {
        return new Auction(id, new Art("P1", "Demo", 100, "Artist"),
                new Seller("S1", "seller", "demo", "seller@example.com"), Instant.now().plusSeconds(120));
    }

    private BidDTO toDto(String auctionId, Bid bid) {
        return new BidDTO(bid.getBidId(), auctionId, bid.getBidderId(), bid.getAmount(), bid.getBidTime());
    }

    private NetworkMessage roundTrip(NetworkMessage message) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(message);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (NetworkMessage) input.readObject();
        }
    }
}
