package com.auction.app.client.data;

import com.auction.app.client.model.AuctionViewData;
import com.auction.app.common.dto.AuctionDTO;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Maps the shared server contract to data used by the JavaFX table.
 */
public final class AuctionDtoMapper {
    private AuctionDtoMapper() {
    }

    public static List<AuctionViewData> toViewData(Collection<AuctionDTO> auctions) {
        return auctions.stream()
                .map(AuctionDtoMapper::toViewData)
                .collect(Collectors.toList());
    }

    public static AuctionViewData toViewData(AuctionDTO auction) {
        return new AuctionViewData(
                auction.auctionId(),
                auction.productName(),
                "—",
                "—",
                formatPrice(auction.currentPrice()),
                0,
                auction.endsAt(),
                formatStatus(auction.status().name()),
                "Dữ liệu từ máy chủ.");
    }

    private static String formatPrice(double price) {
        return String.format(Locale.forLanguageTag("vi-VN"), "%,.0f đ", price);
    }

    private static String formatStatus(String status) {
        return switch (status) {
            case "OPEN" -> "MỞ";
            case "RUNNING" -> "ĐANG DIỄN RA";
            case "FINISHED" -> "ĐÃ KẾT THÚC";
            case "PAID" -> "ĐÃ THANH TOÁN";
            case "CANCELED" -> "ĐÃ HỦY";
            default -> status;
        };
    }
}
