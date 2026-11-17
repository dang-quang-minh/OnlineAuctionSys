package com.auction.app.common.exception;

/**
 * Ngoại lệ khi quá trình xác thực danh tính thất bại (sai thông tin đăng nhập,
 * chưa đăng nhập hoặc phiên làm việc đã hết hạn).
 */
public class AuthenticationException extends BaseAppException {
  private static final long serialVersionUID = 1L;

  public static final String DEFAULT_ERROR_CODE = "AUTH_ERROR";

  public AuthenticationException(String message) {
    super(DEFAULT_ERROR_CODE, message);
  }

  public AuthenticationException(String errorCode, String message) {
    super(errorCode, message);
  }

  public AuthenticationException(String message, Throwable cause) {
    super(DEFAULT_ERROR_CODE, message, cause);
  }
}
