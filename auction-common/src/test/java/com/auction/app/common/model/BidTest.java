package com.auction.app.common.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class BidTest {

    @Test
    public void shouldCreateBidSuccessfully() {
        LocalDateTime now = LocalDateTime.now();

        Bid bid = new Bid(
                "USER001",
                1500000,
                now
        );

        assertEquals(
                "USER001",
                bid.getBidderId()
        );
        assertEquals(
                1500000,
                bid.getAmount()
        );
        assertEquals(
                now,
                bid.getBidTime()
        );
    }

    @Test
    public void shouldRejectNegativeAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Bid("USER001", -1)
        );

    }

    @Test
    public void shouldRejectNullBidderId() {
        assertThrows(
                NullPointerException.class,
                () -> new Bid(null,1000000)
        );
    }
}
