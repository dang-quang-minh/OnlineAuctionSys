package com.auction.app.common.exception;

/**
 * Ngoại lệ khi lượt đặt giá vi phạm quy tắc đấu giá (số tiền không hữu hạn, âm,
 * bằng 0, hoặc nhỏ hơn hay bằng mức giá hiện tại).
 */
public class InvalidBidException extends BaseAppException {
  private static final long serialVersionUID = 1L;

  public static final String DEFAULT_ERROR_CODE = "INVALID_BID";

  public InvalidBidException(String message) {
    super(DEFAULT_ERROR_CODE, message);
  }

  public InvalidBidException(double currentPrice, double attemptedBid) {
    super(
            DEFAULT_ERROR_CODE,
            String.format("Giá đặt (%.2f) phải lớn hơn giá hiện tại (%.2f)",
                    attemptedBid,
                    currentPrice));
  }

  public InvalidBidException(String message, Throwable cause) {
    super(DEFAULT_ERROR_CODE, message, cause);
  }
}
