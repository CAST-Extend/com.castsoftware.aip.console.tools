package com.castsoftware.uc.aip.console.tools.core.exceptions;

public class ApplicationServiceException extends Exception {
    public ApplicationServiceException() {
        super();
    }

    public ApplicationServiceException(String message) {
        super(message);
    }

    public ApplicationServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApplicationServiceException(Throwable cause) {
        super(cause);
    }
}
