package com.castsoftware.uc.aip.console.tools.core.exceptions;

public class JobServiceException extends Exception {
    public JobServiceException() {
        super();
    }

    public JobServiceException(String message) {
        super(message);
    }

    public JobServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public JobServiceException(Throwable cause) {
        super(cause);
    }
}
