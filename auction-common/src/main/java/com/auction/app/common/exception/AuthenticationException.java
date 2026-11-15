package com.auction.app.common.exception;

/**
 * Ngoại lệ khi xác thực người dùng thất bại.
 */
public class AuthenticationException extends BaseAppException {
  private static final long serialVersionUID = 1L;

  public AuthenticationException(String message) {
    super("AUTH_ERROR", message);
  }

  public AuthenticationException(String errorCode, String message) {
    super(errorCode, message);
  }
}
