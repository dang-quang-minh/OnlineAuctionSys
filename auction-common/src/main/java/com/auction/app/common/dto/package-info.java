/**
 * Các DTO dùng để trao đổi dữ liệu qua mạng.
 *
 * <p>Quy ước message và payload:</p>
 * <ul>
 *   <li>{@link NetworkMessage#LOGIN}: client gửi {@link LoginDTO} với các
 *       trường {@code username}, {@code password}.</li>
 *   <li>{@link NetworkMessage#LOGIN_RESULT}: server gửi
 *       {@link LoginResultDTO} với {@code success}, {@code message}.</li>
 *   <li>{@link NetworkMessage#AUCTION_LIST}: server gửi
 *       {@code ArrayList<AuctionDTO>}; mỗi DTO có {@code auctionId},
 *       {@code productName}, {@code currentPrice}, {@code status},
 *       {@code endsAt}.</li>
 *   <li>{@link NetworkMessage#PLACE_BID}: client gửi {@link BidRequestDTO} với
 *       {@code auctionId}, {@code bidderId}, {@code amount}.</li>
 *   <li>{@link NetworkMessage#NEW_BID}: server gửi {@link BidDTO} sau khi bid
 *       được nhận thành công; server sinh {@code bidId}, {@code bidTime}.</li>
 *   <li>{@link NetworkMessage#ERROR}: server gửi thông báo lỗi dạng
 *       {@link String}.</li>
 * </ul>
 *
 * <p>{@link AuctionDTO} do server tạo từ trạng thái phiên để gửi cho client.
 * Không gửi trực tiếp model, product, user hoặc JavaFX object qua mạng.</p>
 */
package com.auction.app.common.dto;
