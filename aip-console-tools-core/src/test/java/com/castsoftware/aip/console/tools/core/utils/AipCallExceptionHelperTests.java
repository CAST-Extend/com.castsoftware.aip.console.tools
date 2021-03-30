package com.castsoftware.aip.console.tools.core.utils;

import com.castsoftware.aip.console.tools.core.exceptions.AipCallExceptionHelper;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallNoStackTraceException;
import com.castsoftware.aip.console.tools.core.exceptions.UploadException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.InvocationTargetException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class AipCallExceptionHelperTests {

    @Test
    public void testApiCallException_StatusAndMessageVverbose() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        ApiCallException thisException = AipCallExceptionHelper.getThrowableApiCallException(true, 201, "Unable to login to AIP Console");
        assertThat(thisException instanceof ApiCallException, is(true));
    }

    @Test
    public void testApiCallException_StatusVerbose() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        ApiCallException thisException = AipCallExceptionHelper.getThrowableApiCallException(true, 201);
        assertThat(thisException instanceof ApiCallException, is(true));
    }

    @Test
    public void testApiCallException_StatusAndMessageAndThrowableVerbose() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        ApiCallException thisException = AipCallExceptionHelper.getThrowableApiCallException(true, 201, "Unable to login to AIP Console", new UploadException("Bla bla"));
        assertThat(thisException instanceof ApiCallException, is(true));
    }

    @Test
    public void testApiCallException_StatusAndThrowableVerbose() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        ApiCallException thisException = AipCallExceptionHelper.getThrowableApiCallException(true, 201, new UploadException("Bla bla"));
        assertThat(thisException instanceof ApiCallException, is(true));
    }

    @Test
    public void testApiCallException_StatusAndMessageNoVerbose() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        ApiCallException thisException = AipCallExceptionHelper.getThrowableApiCallException(false, 201, "Unable to login to AIP Console");
        assertThat(thisException instanceof ApiCallNoStackTraceException, is(true));
    }

    @Test
    public void testApiCallException_StatusNoVerbose() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        ApiCallException thisException = AipCallExceptionHelper.getThrowableApiCallException(false, 201);
        assertThat(thisException instanceof ApiCallNoStackTraceException, is(true));
    }

    @Test
    public void testApiCallException_StatusAndMessageAndThrowableNoVerbose() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        ApiCallException thisException = AipCallExceptionHelper.getThrowableApiCallException(false, 201, "Unable to login to AIP Console", new UploadException("Bla bla"));
        assertThat(thisException instanceof ApiCallNoStackTraceException, is(true));
    }

    @Test
    public void testApiCallException_StatusAndThrowableNoVerbose() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        ApiCallException thisException = AipCallExceptionHelper.getThrowableApiCallException(false, 201, new UploadException("Bla bla"));
        assertThat(thisException instanceof ApiCallNoStackTraceException, is(true));
    }
}
