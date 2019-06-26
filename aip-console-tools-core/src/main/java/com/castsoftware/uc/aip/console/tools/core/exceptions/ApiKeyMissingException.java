package com.castsoftware.uc.aip.console.tools.core.exceptions;

public class ApiKeyMissingException extends ApiCallException {
    public ApiKeyMissingException() {
        super(401);
    }

    public ApiKeyMissingException(String message) {
        super(401, message);
    }
}
