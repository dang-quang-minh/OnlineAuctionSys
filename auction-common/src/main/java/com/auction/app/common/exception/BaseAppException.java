package com.auction.app.common.exception;

/**
 * Ngoại lệ cơ sở cho các lỗi nghiệp vụ và xác thực trong toàn bộ hệ thống đấu giá.
 * Chuẩn hóa {@code errorCode} và thông điệp thân thiện với người dùng, đồng thời giữ nguyên
 * {@code cause} khi bọc các lỗi kỹ thuật.
 */
public class BaseAppException extends Exception {

  private static final long serialVersionUID = 1L;

  private final String errorCode;

  public BaseAppException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public BaseAppException(String errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
