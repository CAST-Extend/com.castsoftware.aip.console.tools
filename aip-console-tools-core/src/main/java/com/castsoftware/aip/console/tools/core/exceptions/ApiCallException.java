package com.castsoftware.aip.console.tools.core.exceptions;

public class ApiCallException extends Exception {
    protected int httpStatus = 500;

    public ApiCallException(int httpStatus) {
        super();
        this.httpStatus = httpStatus;
    }

    public ApiCallException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public ApiCallException(int httpStatus, Throwable cause) {
        super(cause);
        this.httpStatus = httpStatus;
    }

    public ApiCallException(int httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    protected ApiCallException(String message, Throwable cause, boolean enableSuppression, boolean enableStackTrace) {
        super(message, cause, enableSuppression, enableStackTrace);
    }

    protected ApiCallException(int httpStatus, String message, Throwable cause, boolean enableSuppression, boolean enableStackTrace) {
        super(message, cause, enableSuppression, enableStackTrace);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
