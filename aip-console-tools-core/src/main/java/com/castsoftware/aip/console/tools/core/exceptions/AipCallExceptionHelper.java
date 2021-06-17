package com.castsoftware.aip.console.tools.core.exceptions;

import java.lang.reflect.InvocationTargetException;

public final class AipCallExceptionHelper {
    private static Class<? extends Exception> getCallExceptionClass(boolean verbose) {
        return verbose ? ApiCallException.class : ApiCallNoStackTraceException.class;
    }

    public static ApiCallException getThrowableApiCallException(boolean verbose, int httpStatus) {
        try {
            return (ApiCallException) getCallExceptionClass(verbose).getConstructor(int.class).newInstance(httpStatus);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return new ApiCallException(httpStatus, e); //here with call stack anyway
        }
    }

    public static ApiCallException getThrowableApiCallException(boolean verbose, int httpStatus, String message) {
        try {
            return (ApiCallException) getCallExceptionClass(verbose).getConstructor(int.class, String.class).newInstance(httpStatus, message);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return new ApiCallException(httpStatus, e); //here with call stack anyway
        }
    }

    public static ApiCallException getThrowableApiCallException(boolean verbose, int httpStatus, Throwable cause) {
        try {
            return (ApiCallException) getCallExceptionClass(verbose).getConstructor(int.class, Throwable.class).newInstance(httpStatus, cause);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return new ApiCallException(httpStatus, e); //here with call stack anyway
        }
    }

    public static ApiCallException getThrowableApiCallException(boolean verbose, ApiCallException cause) {
        return getThrowableApiCallException(verbose, cause.getHttpStatus(), cause);
    }

    public static ApiCallException getThrowableApiCallException(boolean verbose, int httpStatus, String message, Throwable cause) {
        try {
            return (ApiCallException) getCallExceptionClass(verbose).getConstructor(int.class, String.class, Throwable.class).newInstance(httpStatus, message, cause);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return new ApiCallException(httpStatus, e); //here with call stack anyway
        }
    }
}
