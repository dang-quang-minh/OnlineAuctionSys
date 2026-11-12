package com.auction.app.common.exception;
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
