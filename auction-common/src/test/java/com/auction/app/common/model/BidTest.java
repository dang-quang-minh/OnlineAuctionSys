package com.auction.app.common.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class BidTest {

    @Test
    public void shouldCreateBidSuccessfully() {
        Instant now = Instant.now();

        Bid bid = new Bid(
                "BID001", "USER001",
                1500000,now
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
                () -> new Bid("BID001","USER001", -1,Instant.now())
        );

    }

    @Test
    public void shouldRejectNullBidderId() {
        assertThrows(
                NullPointerException.class,
                () -> new Bid("BID001",null,1000000,Instant.now())
        );
    }
}
