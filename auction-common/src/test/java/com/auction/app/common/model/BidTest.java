package com.auction.app.common.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BidTest {

    @Test
    void shouldCreateBidAndPreserveAllFields() {
        Instant now = Instant.now();

        Bid bid = new Bid("BID001", "USER001", 1_500_000, now);

        assertEquals("BID001", bid.getBidId());
        assertEquals("USER001", bid.getBidderId());
        assertEquals(1_500_000, bid.getAmount());
        assertEquals(now, bid.getBidTime());
    }

    @Test
    void shouldAllowZeroAmountAtBidValueObjectLevel() {
        Bid bid = new Bid("BID001", "USER001", 0, Instant.now());

        assertEquals(0, bid.getAmount());
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> new Bid("BID001", "USER001", -1, Instant.now()));
    }

    @Test
    void shouldRejectNaNAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> new Bid("BID001", "USER001", Double.NaN, Instant.now()));
    }

    @Test
    void shouldRejectPositiveInfinityAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> new Bid("BID001", "USER001", Double.POSITIVE_INFINITY, Instant.now()));
    }

    @Test
    void shouldRejectNegativeInfinityAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> new Bid("BID001", "USER001", Double.NEGATIVE_INFINITY, Instant.now()));
    }

    @Test
    void shouldRejectNullBidId() {
        assertThrows(NullPointerException.class,
                () -> new Bid(null, "USER001", 1_500_000, Instant.now()));
    }

    @Test
    void shouldRejectNullBidderId() {
        assertThrows(NullPointerException.class,
                () -> new Bid("BID001", null, 1_500_000, Instant.now()));
    }

    @Test
    void shouldRejectNullBidTime() {
        assertThrows(NullPointerException.class,
                () -> new Bid("BID001", "USER001", 1_500_000, null));
    }
}
