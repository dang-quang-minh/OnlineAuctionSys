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
import com.auction.app.common.observer.Observer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
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
import static org.mockito.Mockito.spy;
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

    @Test
    void overdueOpenAuctionKeepsOnePendingCheckUntilItStarts() {
        Auction auction = auction("A1", AuctionStatus.OPEN);
        AtomicReference<AuctionStatus> state = new AtomicReference<>(AuctionStatus.OPEN);
        when(auction.getStatus()).thenAnswer(call -> state.get());
        doAnswer(call -> {
            state.set(AuctionStatus.FINISHED);
            return null;
        }).when(auction).finish();
        manager.createAuction(auction);

        // Đến hạn nhưng chưa start: không ép chuyển trạng thái, vẫn giữ một lịch kiểm tra tiếp.
        deadlineTask().run();
        verify(auction, never()).finish();
        List<Runnable> tasks = deadlineTasks(2);
        verify(scheduler).schedule(any(Runnable.class), eq(TimeUnit.MILLISECONDS.toNanos(100)),
                eq(TimeUnit.NANOSECONDS));

        tasks.get(1).run();
        verify(auction, never()).finish();
        tasks = deadlineTasks(3);

        // Mô phỏng model được start trễ; lần kiểm tra tiếp phải đóng phiên và dừng retry.
        state.set(AuctionStatus.RUNNING);
        tasks.get(2).run();
        verify(auction).finish();
        deadlineTasks(3);
        assertFalse(manager.getActiveAuctions().containsKey("A1"));
    }

    @Test
    void pendingOpenCheckStopsWhenAuctionReachesTerminalState() {
        Auction auction = auction("A1", AuctionStatus.OPEN);
        manager.createAuction(auction);
        deadlineTask().run();
        Runnable pendingCheck = deadlineTasks(2).get(1);
        when(auction.getStatus()).thenReturn(AuctionStatus.CANCELED);

        pendingCheck.run();

        verify(auction, never()).finish();
        deadlineTasks(2);
    }

    @Test
    void closeDuringOpenStateReadPreventsAnotherCheck() throws Exception {
        Auction auction = auction("A1", AuctionStatus.OPEN);
        CountDownLatch stateReadStarted = new CountDownLatch(1);
        CountDownLatch releaseStateRead = new CountDownLatch(1);
        when(auction.getStatus()).thenAnswer(call -> {
            stateReadStarted.countDown();
            assertTrue(releaseStateRead.await(5, TimeUnit.SECONDS));
            return AuctionStatus.OPEN;
        });
        manager.createAuction(auction);
        Runnable task = deadlineTask();
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Future<?> runningTask = worker.submit(task);
            assertTrue(stateReadStarted.await(5, TimeUnit.SECONDS));
            manager.close();
            releaseStateRead.countDown();
            runningTask.get(5, TimeUnit.SECONDS);

            deadlineTasks(1);
            verify(auction, never()).finish();
        } finally {
            releaseStateRead.countDown();
            worker.shutdownNow();
        }
    }

    @Test
    void creationWaitingForLockRecomputesDelayBeforeScheduling() throws Exception {
        ScheduledExecutorService blockedScheduler = mock(ScheduledExecutorService.class);
        Clock movingClock = mock(Clock.class);
        AtomicReference<Instant> currentTime = new AtomicReference<>(NOW);
        when(movingClock.instant()).thenAnswer(call -> currentTime.get());
        CountDownLatch firstScheduleEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstSchedule = new CountDownLatch(1);
        AtomicInteger scheduleCalls = new AtomicInteger();
        List<Long> delays = new CopyOnWriteArrayList<>();
        when(blockedScheduler.schedule(any(Runnable.class), anyLong(), eq(TimeUnit.NANOSECONDS)))
                .thenAnswer(call -> {
                    if (scheduleCalls.incrementAndGet() == 1) {
                        firstScheduleEntered.countDown();
                        assertTrue(releaseFirstSchedule.await(5, TimeUnit.SECONDS));
                    }
                    delays.add(call.getArgument(1));
                    return null;
                });
        Auction firstAuction = auction("A1", AuctionStatus.RUNNING);
        Auction secondAuction = auction("A2", AuctionStatus.RUNNING);
        when(firstAuction.getEndsAt()).thenReturn(NOW.plusSeconds(10));
        when(secondAuction.getEndsAt()).thenReturn(NOW.plusSeconds(10));
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try (AuctionManager localManager = new AuctionManager(blockedScheduler, movingClock)) {
            FutureTask<Auction> secondCreation = new FutureTask<>(() -> localManager.createAuction(secondAuction));
            Thread secondThread = new Thread(secondCreation, "waiting-auction-creation");
            try {
                Future<Auction> firstCreation = worker.submit(() -> localManager.createAuction(firstAuction));
                assertTrue(firstScheduleEntered.await(5, TimeUnit.SECONDS));
                secondThread.start();
                long waitUntil = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while (secondThread.getState() != Thread.State.BLOCKED && System.nanoTime() < waitUntil) {
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
                }
                assertEquals(Thread.State.BLOCKED, secondThread.getState(), "Luồng tạo phiên phải đang chờ khóa.");
                currentTime.set(NOW.plusSeconds(20));
                releaseFirstSchedule.countDown();
                firstCreation.get(5, TimeUnit.SECONDS);
                secondCreation.get(5, TimeUnit.SECONDS);

                assertEquals(List.of(TimeUnit.SECONDS.toNanos(10), 0L), delays);
            } finally {
                releaseFirstSchedule.countDown();
                secondThread.interrupt();
                worker.shutdownNow();
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = AuctionStatus.class, names = {"FINISHED", "PAID", "CANCELED"})
    void deadlineDoesNotForceInvalidStateTransition(AuctionStatus status) {
        Auction auction = auction("A1", status);
        manager.createAuction(auction);

        deadlineTask().run();

        verify(auction, never()).finish();
        deadlineTasks(1);
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

    @Test
    void realExecutorClosesModelStartedAfterDeadline() throws Exception {
        ScheduledExecutorService realScheduler = Executors.newSingleThreadScheduledExecutor();
        Instant endsAt = Instant.now().plusMillis(300);
        Auction auction = spy(new Auction("late-start",
                new Art("P1", "Demo", 100, "Artist"),
                new Seller("S1", "seller", "demo", "seller@example.com"), endsAt));
        CountDownLatch expiredOpenObserved = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        doAnswer(call -> {
            AuctionStatus status = (AuctionStatus) call.callRealMethod();
            if (status == AuctionStatus.OPEN && !Instant.now().isBefore(endsAt)) {
                expiredOpenObserved.countDown();
            }
            return status;
        }).when(auction).getStatus();
        doAnswer(call -> {
            call.callRealMethod();
            finished.countDown();
            return null;
        }).when(auction).finish();

        try (AuctionManager localManager = new AuctionManager(realScheduler, Clock.systemUTC())) {
            localManager.createAuction(auction);
            assertTrue(expiredOpenObserved.await(5, TimeUnit.SECONDS), "Phải quan sát OPEN sau hạn.");
            assertEquals(AuctionStatus.OPEN, auction.getStatus());
            auction.start();

            assertTrue(finished.await(5, TimeUnit.SECONDS), "Phiên start trễ vẫn phải được tự đóng.");
            assertEquals(AuctionStatus.FINISHED, auction.getStatus());
            verify(auction).finish();
            assertThrows(AuctionClosedException.class,
                    () -> localManager.processBid("late-start", new BidRequestDTO("late-start", "U1", 150)));
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
        return deadlineTasks(1).get(0);
    }

    private List<Runnable> deadlineTasks(int count) {
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler, times(count)).schedule(task.capture(), anyLong(), eq(TimeUnit.NANOSECONDS));
        return task.getAllValues();
    }
}
