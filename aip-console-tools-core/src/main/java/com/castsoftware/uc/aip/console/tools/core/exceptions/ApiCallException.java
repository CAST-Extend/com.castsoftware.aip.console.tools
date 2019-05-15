package com.castsoftware.uc.aip.console.tools.core.exceptions;

public class ApiCallException extends Exception {

    public ApiCallException() {
        super();
    }

    public ApiCallException(String message) {
        super(message);
    }

    public ApiCallException(Throwable cause) {
        super(cause);
    }

    public ApiCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
