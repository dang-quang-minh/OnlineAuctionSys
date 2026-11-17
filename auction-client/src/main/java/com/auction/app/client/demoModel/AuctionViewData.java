package com.auction.app.client.demoModel;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * View data used by the JavaFX auction list.
 *
 * <p>This shape is intentionally independent from the server model and can
 * later be populated from AuctionDTO.</p>
 */
public record AuctionViewData(
        String auctionId,
        String productName,
        String seller,
        String startingPrice,
        String currentPrice,
        int bidCount,
        Instant endsAt,
        String status,
        String description) {
    private static final DateTimeFormatter END_TIME_FORMAT = DateTimeFormatter
            .ofPattern("HH:mm, dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
            .withZone(ZoneId.systemDefault());

    public String getAuctionId() {
        return auctionId;
    }

    public String getProductName() {
        return productName;
    }

    public String getSeller() {
        return seller;
    }

    public String getStartingPrice() {
        return startingPrice;
    }

    public String getCurrentPrice() {
        return currentPrice;
    }

    public int getBidCount() {
        return bidCount;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public String getEndTime() {
        return END_TIME_FORMAT.format(endsAt);
    }

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public String getAction() {
        return "Xem";
    }
}
