package com.auction.app.common.exception;

/**
 * Ngoại lệ khi phiên đấu giá đã đóng hoặc chưa bắt đầu.
 */
public class AuctionClosedException extends BaseAppException {
  private static final long serialVersionUID = 1L;

  public AuctionClosedException(String message) {
    super("AUCTION_CLOSED", message);
  }

  public AuctionClosedException(String auctionId, String status) {
    super("AUCTION_CLOSED", String.format("Phiên đấu giá '%s' hiện không thể giao dịch (Trạng thái: %s)", auctionId, status));
  }
}
