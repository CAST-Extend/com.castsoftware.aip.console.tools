package com.castsoftware.aip.console.tools.core.exceptions;

public class HiddenStackTraceException extends Exception {
    private static final String EMPTY_MESSAGE = "";
    private static final boolean ENABLE_STACK_TRACE = false;
    private static final boolean ENABLE_SUPPRESSION = false;

    public HiddenStackTraceException() {
        super(EMPTY_MESSAGE, null, ENABLE_SUPPRESSION, ENABLE_STACK_TRACE);
    }

    public HiddenStackTraceException(Throwable cause) {
        super(cause.toString(), cause, ENABLE_SUPPRESSION, ENABLE_STACK_TRACE);
    }

    public HiddenStackTraceException(String message, Throwable cause) {
        super(message, cause, ENABLE_SUPPRESSION, ENABLE_STACK_TRACE);
    }

    public HiddenStackTraceException(String message) {
        super(message, null, ENABLE_SUPPRESSION, ENABLE_STACK_TRACE);
    }

}
