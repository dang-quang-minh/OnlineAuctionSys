package com.auction.app.common.exception;

/**
 * Ngoại lệ khi mức giá đặt không hợp lệ.
 */
public class InvalidBidException extends BaseAppException {
  private static final long serialVersionUID = 1L;

  public InvalidBidException(String message) {
    super("INVALID_BID", message);
  }

  public InvalidBidException(double currentPrice, double attemptedBid) {
    super("INVALID_BID", String.format("Giá đặt (%.2f) phải lớn hơn giá hiện tại (%.2f)", attemptedBid, currentPrice));
  }
}
