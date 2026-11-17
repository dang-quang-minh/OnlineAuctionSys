package com.auction.app.common.exception;

/**
 * Ngoại lệ khi thao tác giao dịch trên phiên đấu giá không ở trạng thái {@code RUNNING}
 * (chưa bắt đầu, đã kết thúc, đã hủy) hoặc đã vượt quá thời điểm {@code endsAt}.
 */
public class AuctionClosedException extends BaseAppException {
  private static final long serialVersionUID = 1L;

  public static final String DEFAULT_ERROR_CODE = "AUCTION_CLOSED";

  public AuctionClosedException(String message) {
    super(DEFAULT_ERROR_CODE, message);
  }

  public AuctionClosedException(String auctionId, String status) {
    super(DEFAULT_ERROR_CODE, String.format("Phiên đấu giá '%s' hiện không thể giao dịch (Trạng thái: %s)", auctionId, status));
  }

  public AuctionClosedException(String message, Throwable cause) {
    super(DEFAULT_ERROR_CODE, message, cause);
  }
}
