package com.castsoftware.aip.console.tools.core.exceptions;

import java.lang.reflect.InvocationTargetException;

public final class AipCallExceptionHelper {
    private static Class<? extends Exception> getCallExceptionClass(boolean verbose) {
        return verbose ? ApiCallException.class : ApiCallNoStackTraceException.class;
    }

    public static ApiCallException getThrowableApiCallException(boolean verbose, int httpStatus) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return (ApiCallException) getCallExceptionClass(verbose).getConstructor(int.class).newInstance(httpStatus);
    }

    public static ApiCallException getThrowableApiCallException(boolean verbose, int httpStatus, String message) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return (ApiCallException) getCallExceptionClass(verbose).getConstructor(int.class, String.class).newInstance(httpStatus, message);
    }

    public static ApiCallException getThrowableApiCallException(boolean verbose, int httpStatus, Throwable cause) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return (ApiCallException) getCallExceptionClass(verbose).getConstructor(int.class, Throwable.class).newInstance(httpStatus, cause);
    }

    public static ApiCallException getThrowableApiCallException(boolean verbose, int httpStatus, String message, Throwable cause) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return (ApiCallException) getCallExceptionClass(verbose).getConstructor(int.class, String.class, Throwable.class).newInstance(httpStatus, message, cause);
    }
}
