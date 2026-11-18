package com.auction.app.client.demoData;

import com.auction.app.client.demoModel.AuctionViewData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoAuctionDataTest {
    @Test
    void loadsViewDataForTheDemoList() {
        List<AuctionViewData> auctions = DemoAuctionData.load();

        assertFalse(auctions.isEmpty());
        assertTrue(auctions.stream().allMatch(
                auction -> auction.getAuctionId() != null
                        && !auction.getAuctionId().isBlank()));
    }
}
