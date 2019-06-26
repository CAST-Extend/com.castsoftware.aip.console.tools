package com.castsoftware.uc.aip.console.tools.core.exceptions;

public class ApiCallException extends Exception {
    private int httpStatus = 500;

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

    public int getHttpStatus() {
        return httpStatus;
    }
}
