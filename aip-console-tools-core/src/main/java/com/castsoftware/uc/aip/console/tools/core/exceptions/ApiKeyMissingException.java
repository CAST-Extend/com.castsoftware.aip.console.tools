package com.castsoftware.uc.aip.console.tools.core.exceptions;

public class ApiKeyMissingException extends ApiCallException {
    public ApiKeyMissingException() {
        super();
    }

    public ApiKeyMissingException(String message) {
        super(message);
    }
}
