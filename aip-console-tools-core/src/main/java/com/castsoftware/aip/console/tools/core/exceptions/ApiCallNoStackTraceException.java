package com.castsoftware.aip.console.tools.core.exceptions;

public class ApiCallNoStackTraceException extends ApiCallException {
    private static final String EMPTY_MESSAGE = "";
    private static final boolean ENABLE_STACK_TRACE = false;
    private static final boolean ENABLE_SUPPRESSION = false;

    public ApiCallNoStackTraceException() {
        super(EMPTY_MESSAGE, null, ENABLE_SUPPRESSION, ENABLE_STACK_TRACE);
    }

    public ApiCallNoStackTraceException(Throwable cause) {
        super(cause.toString(), cause, ENABLE_SUPPRESSION, ENABLE_STACK_TRACE);
    }

    public ApiCallNoStackTraceException(String message, Throwable cause) {
        super(message, cause, ENABLE_SUPPRESSION, ENABLE_STACK_TRACE);
    }

    public ApiCallNoStackTraceException(String message) {
        super(message, null, ENABLE_SUPPRESSION, ENABLE_STACK_TRACE);
    }

    public ApiCallNoStackTraceException(int httpStatus) {
        this();
        this.httpStatus = httpStatus;
    }

    public ApiCallNoStackTraceException(int httpStatus, String message) {
        this(httpStatus, message, null);
    }

    public ApiCallNoStackTraceException(int httpStatus, Throwable cause) {
        this(httpStatus, cause.toString(), cause);
    }

    public ApiCallNoStackTraceException(int httpStatus, String message, Throwable cause) {
        super(httpStatus, message, cause, ENABLE_SUPPRESSION, ENABLE_STACK_TRACE);
    }
}
