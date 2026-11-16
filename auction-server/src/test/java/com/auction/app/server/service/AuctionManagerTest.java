package com.auction.app.server.service;

import com.auction.app.common.dto.BidDTO;
import com.auction.app.common.dto.BidRequestDTO;
import com.auction.app.common.dto.NetworkMessage;
import com.auction.app.common.exception.AuctionClosedException;
import com.auction.app.common.exception.InvalidBidException;
import com.auction.app.common.model.Auction;
import com.auction.app.common.model.AuctionStatus;
import com.auction.app.common.model.Bid;
import com.auction.app.common.observer.Observer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Chỉ test orchestration; quy tắc giá/trạng thái và khóa do Job 1 kiểm thử. */
class AuctionManagerTest {

    private static final Instant NOW = Instant.parse("2026-11-16T16:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private ScheduledExecutorService scheduler;
    private AuctionManager manager;

    @BeforeEach
    void setUp() {
        scheduler = mock(ScheduledExecutorService.class);
        manager = new AuctionManager(scheduler, CLOCK);
    }

    @AfterEach
    void tearDown() {
        manager.close();
    }

    @Test
    void createsAndLooksUpAuctionWithoutExposingInternalMap() {
        Auction auction = auction("A1", AuctionStatus.OPEN);

        assertSame(auction, manager.createAuction(auction));
        assertSame(auction, manager.getAuction("A1").orElseThrow());
        assertTrue(manager.getAuction("missing").isEmpty());
        manager.getAllAuctions().clear();
        assertTrue(manager.getAuction("A1").isPresent());
    }

    @Test
    void duplicateIdDoesNotReplaceOriginalOrCreateAnotherTimer() {
        Auction original = auction("A1", AuctionStatus.RUNNING);
        manager.createAuction(original);

        assertThrows(IllegalArgumentException.class,
                () -> manager.createAuction(auction("A1", AuctionStatus.OPEN)));

        assertSame(original, manager.getAuction("A1").orElseThrow());
        verify(scheduler).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.NANOSECONDS));
    }

    @Test
    void activeSnapshotIncludesOnlyOpenAndRunningAndCannotChangeRegistry() {
        for (AuctionStatus status : AuctionStatus.values()) {
            manager.createAuction(auction(status.name(), status));
        }
        Map<String, Auction> snapshot = manager.getActiveAuctions();

        assertEquals(2, snapshot.size());
        assertTrue(snapshot.containsKey("OPEN"));
        assertTrue(snapshot.containsKey("RUNNING"));
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        manager.createAuction(auction("new", AuctionStatus.OPEN));
        assertFalse(snapshot.containsKey("new"));
    }

    @Test
    void successfulBidCallsModelOnceAndPublishesExactReturnedBid() throws Exception {
        Auction auction = auction("A1", AuctionStatus.RUNNING);
        Bid bid = bid("server-bid-id", "U1", 150, NOW.minusSeconds(1));
        when(auction.placeBid(150, "U1")).thenReturn(bid);
        manager.createAuction(auction);
        Observer observer = mock(Observer.class);
        manager.registerObserver(observer);

        BidDTO result = manager.processBid("A1", new BidRequestDTO("A1", "U1", 150));

        assertEquals(new BidDTO("server-bid-id", "A1", "U1", 150, NOW.minusSeconds(1)), result);
        verify(auction).placeBid(150, "U1");
        ArgumentCaptor<NetworkMessage> event = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(observer).update(event.capture());
        assertEquals(NetworkMessage.NEW_BID, event.getValue().getType());
        assertSame(result, event.getValue().getPayload());
    }

    @Test
    void invalidBidPropagatesSameExceptionAndDoesNotPublish() throws Exception {
        Auction auction = auction("A1", AuctionStatus.RUNNING);
        InvalidBidException failure = new InvalidBidException("Giá đặt quá thấp.");
        when(auction.placeBid(50, "U1")).thenThrow(failure);
        manager.createAuction(auction);
        Observer observer = mock(Observer.class);
        manager.registerObserver(observer);

        assertSame(failure, assertThrows(InvalidBidException.class,
                () -> manager.processBid("A1", new BidRequestDTO("A1", "U1", 50))));

        verify(auction).placeBid(50, "U1");
        verifyNoInteractions(observer);
    }

    @Test
    void closedAuctionPropagatesModelExceptionAndDoesNotPublish() throws Exception {
        Auction auction = auction("A1", AuctionStatus.FINISHED);
        AuctionClosedException failure = new AuctionClosedException("Phiên đã kết thúc.");
        when(auction.placeBid(150, "U1")).thenThrow(failure);
        manager.createAuction(auction);
        Observer observer = mock(Observer.class);
        manager.registerObserver(observer);

        assertSame(failure, assertThrows(AuctionClosedException.class,
                () -> manager.processBid("A1", new BidRequestDTO("A1", "U1", 150))));

        verifyNoInteractions(observer);
    }

    @Test
    void missingOrMismatchedAuctionIdNeverCallsModelOrPublishes() throws Exception {
        Auction auction = auction("A1", AuctionStatus.RUNNING);
        manager.createAuction(auction);
        Observer observer = mock(Observer.class);
        manager.registerObserver(observer);

        assertThrows(IllegalArgumentException.class,
                () -> manager.processBid("missing", new BidRequestDTO("missing", "U1", 150)));
        assertThrows(IllegalArgumentException.class,
                () -> manager.processBid("A1", new BidRequestDTO("other", "U1", 150)));

        verify(auction, never()).placeBid(anyDouble(), anyString());
        verifyNoInteractions(observer);
    }

    @Test
    void duplicateRegistrationAndRemovalDoNotProduceDuplicateUpdates() {
        Observer observer = mock(Observer.class);
        NetworkMessage message = new NetworkMessage(NetworkMessage.ERROR, "demo");
        manager.registerObserver(observer);
        manager.registerObserver(observer);

        manager.notifyObservers(message);
        manager.removeObserver(observer);
        manager.notifyObservers(message);

        verify(observer).update(message);
    }

    @Test
    void observerCanUnsubscribeAndRegisterAnotherObserverDuringNotification() {
        Observer nextObserver = mock(Observer.class);
        Observer currentObserver = new Observer() {
            @Override
            public void update(NetworkMessage message) {
                manager.removeObserver(this);
                manager.registerObserver(nextObserver);
            }
        };
        manager.registerObserver(currentObserver);
        NetworkMessage message = new NetworkMessage(NetworkMessage.ERROR, "demo");

        manager.notifyObservers(message);
        verifyNoInteractions(nextObserver);
        manager.notifyObservers(message);
        verify(nextObserver).update(message);
    }

    @Test
    void failingObserverDoesNotHideAcceptedBidOrBlockOtherObservers() throws Exception {
        Auction auction = auction("A1", AuctionStatus.RUNNING);
        Bid acceptedBid = bid("B1", "U1", 150, NOW);
        when(auction.placeBid(150, "U1")).thenReturn(acceptedBid);
        manager.createAuction(auction);
        Observer broken = mock(Observer.class);
        Observer healthy = mock(Observer.class);
        doThrow(new IllegalStateException("Observer demo bị lỗi.")).when(broken).update(any());
        manager.registerObserver(broken);
        manager.registerObserver(healthy);

        BidDTO result = manager.processBid("A1", new BidRequestDTO("A1", "U1", 150));

        assertEquals("B1", result.bidId());
        verify(broken).update(any(NetworkMessage.class));
        verify(healthy).update(any(NetworkMessage.class));
        verify(auction).placeBid(150, "U1");
    }

    @Test
    void schedulesFinishAtEndsAtWithoutRoundingDeadlineDown() {
        Auction auction = auction("A1", AuctionStatus.RUNNING);
        when(auction.getEndsAt()).thenReturn(NOW.plusSeconds(10).plusNanos(123));

        manager.createAuction(auction);

        verify(scheduler).schedule(any(Runnable.class), eq(10_000_000_123L), eq(TimeUnit.NANOSECONDS));
        verify(auction, never()).finish();
        deadlineTask().run();
        verify(auction).finish();
    }

    @Test
    void pastDeadlineIsScheduledImmediately() {
        Auction auction = auction("A1", AuctionStatus.RUNNING);
        when(auction.getEndsAt()).thenReturn(NOW.minusSeconds(10));

        manager.createAuction(auction);

        verify(scheduler).schedule(any(Runnable.class), eq(0L), eq(TimeUnit.NANOSECONDS));
    }

    @ParameterizedTest
    @EnumSource(value = AuctionStatus.class, names = {"OPEN", "FINISHED", "PAID", "CANCELED"})
    void deadlineDoesNotForceInvalidStateTransition(AuctionStatus status) {
        Auction auction = auction("A1", status);
        manager.createAuction(auction);

        deadlineTask().run();

        verify(auction, never()).finish();
    }

    @Test
    void concurrentFinishDoesNotMakeTimerFail() {
        Auction auction = auction("A1", AuctionStatus.RUNNING);
        doThrow(new IllegalStateException("Luồng khác đã đóng phiên.")).when(auction).finish();
        manager.createAuction(auction);

        assertDoesNotThrow(() -> deadlineTask().run());
    }

    @Test
    void schedulerFailureRollsBackNewAuction() {
        when(scheduler.schedule(any(Runnable.class), anyLong(), eq(TimeUnit.NANOSECONDS)))
                .thenThrow(new RejectedExecutionException("Scheduler đã dừng."));

        assertThrows(RejectedExecutionException.class,
                () -> manager.createAuction(auction("A1", AuctionStatus.RUNNING)));

        assertTrue(manager.getAuction("A1").isEmpty());
    }

    @Test
    void closeStopsTimersAndRejectsNewWrites() throws Exception {
        Auction auction = auction("A1", AuctionStatus.RUNNING);
        manager.createAuction(auction);
        Runnable task = deadlineTask();

        manager.close();
        manager.close();
        task.run();

        verify(scheduler).shutdownNow();
        verify(auction, never()).finish();
        assertThrows(IllegalStateException.class,
                () -> manager.createAuction(auction("A2", AuctionStatus.OPEN)));
        assertThrows(IllegalStateException.class,
                () -> manager.processBid("A1", new BidRequestDTO("A1", "U1", 150)));
        verify(auction, never()).placeBid(anyDouble(), anyString());
    }

    @Test
    void concurrentRequestsKeepTheirOwnBidAndNotifyStableObserverExactlyOnce() throws Exception {
        Auction auction = auction("A1", AuctionStatus.RUNNING);
        int requestCount = 16;
        for (int index = 0; index < requestCount; index++) {
            String bidderId = "U" + index;
            Bid acceptedBid = bid("B" + index, bidderId, 200 + index, NOW);
            when(auction.placeBid(200 + index, bidderId)).thenReturn(acceptedBid);
        }
        manager.createAuction(auction);
        ConcurrentLinkedQueue<NetworkMessage> messages = new ConcurrentLinkedQueue<>();
        manager.registerObserver(messages::add);
        ExecutorService workers = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<BidDTO>> results = new ArrayList<>();
        try {
            for (int index = 0; index < requestCount; index++) {
                int requestIndex = index;
                results.add(workers.submit(() -> {
                    start.await();
                    Observer temporaryObserver = mock(Observer.class);
                    manager.registerObserver(temporaryObserver);
                    try {
                        return manager.processBid("A1",
                                new BidRequestDTO("A1", "U" + requestIndex, 200 + requestIndex));
                    } finally {
                        manager.removeObserver(temporaryObserver);
                    }
                }));
            }
            start.countDown();
            for (int index = 0; index < requestCount; index++) {
                BidDTO result = results.get(index).get(5, TimeUnit.SECONDS);
                assertEquals("B" + index, result.bidId());
                assertEquals("U" + index, result.bidderId());
                assertEquals(200 + index, result.amount());
                assertEquals(1L, messages.stream().filter(message -> message.getPayload() == result).count());
            }
        } finally {
            start.countDown();
            workers.shutdownNow();
        }
        assertEquals(requestCount, messages.size());
        assertTrue(messages.stream().allMatch(message -> NetworkMessage.NEW_BID.equals(message.getType())));
        verify(auction, times(requestCount)).placeBid(anyDouble(), anyString());
    }

    @Test
    void realExecutorRunsDeadlineAndIsShutDownAfterUse() throws Exception {
        ScheduledExecutorService realScheduler = Executors.newSingleThreadScheduledExecutor();
        CountDownLatch finished = new CountDownLatch(1);
        Auction auction = auction("A1", AuctionStatus.RUNNING);
        when(auction.getEndsAt()).thenReturn(NOW.minusSeconds(1));
        doAnswer(invocation -> {
            finished.countDown();
            return null;
        }).when(auction).finish();

        try (AuctionManager localManager = new AuctionManager(realScheduler, CLOCK)) {
            localManager.createAuction(auction);
            assertTrue(finished.await(5, TimeUnit.SECONDS), "Scheduler phải thực sự gọi finish().");
            verify(auction).finish();
        }
        assertTrue(realScheduler.isShutdown());
    }

    private Auction auction(String auctionId, AuctionStatus status) {
        Auction auction = mock(Auction.class);
        when(auction.getAuctionId()).thenReturn(auctionId);
        when(auction.getStatus()).thenReturn(status);
        when(auction.getEndsAt()).thenReturn(NOW.plusSeconds(60));
        return auction;
    }

    private Bid bid(String bidId, String bidderId, double amount, Instant bidTime) {
        Bid bid = mock(Bid.class);
        when(bid.getBidId()).thenReturn(bidId);
        when(bid.getBidderId()).thenReturn(bidderId);
        when(bid.getAmount()).thenReturn(amount);
        when(bid.getBidTime()).thenReturn(bidTime);
        return bid;
    }

    private Runnable deadlineTask() {
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(task.capture(), anyLong(), eq(TimeUnit.NANOSECONDS));
        return task.getValue();
    }
}
