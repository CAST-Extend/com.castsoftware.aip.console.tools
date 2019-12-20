package com.castsoftware.aip.console.tools.core.exceptions;

public class AipConsoleException extends Exception {

    public AipConsoleException() {
        super();
    }

    public AipConsoleException(String message) {
        super(message);
    }

    public AipConsoleException(String message, Throwable t) {
        super(message, t);
    }
}
