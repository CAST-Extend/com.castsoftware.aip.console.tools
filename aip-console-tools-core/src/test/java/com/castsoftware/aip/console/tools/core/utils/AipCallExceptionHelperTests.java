package com.castsoftware.aip.console.tools.core.utils;

import com.castsoftware.aip.console.tools.core.exceptions.AipCallExceptionHelper;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallNoStackTraceException;
import com.castsoftware.aip.console.tools.core.exceptions.ApiKeyMissingException;
import com.castsoftware.aip.console.tools.core.exceptions.UploadException;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class AipCallExceptionHelperTests {

    @Test
    public void testApiCallException_StatusAndMessageVverbose() {
        ApiCallException thisException = AipCallExceptionHelper.getThrowableApiCallException(true, 201, "Unable to login to AIP Console");
        assertThat(thisException instanceof ApiCallException, is(true));
    }

    @Test
    public void testApiCallException_StatusVerbose() {
        ApiCallException thisException = AipCallExceptionHelper.getThrowableApiCallException(true, 201);
        assertThat(thisException instanceof ApiCallException, is(true));
    }

    @Test
    public void testApiCallException_StatusAndMessageAndThrowableVerbose() {
        ApiCallException thisException = AipCallExceptionHelper.getThrowableApiCallException(true, 201, "Unable to login to AIP Console", new UploadException("Bla bla"));
        assertThat(thisException instanceof ApiCallException, is(true));
        assertThat(thisException.getStackTrace().length, greaterThan(1));
        assertThat(StringUtils.isEmpty(thisException.getStackTrace().toString()), Matchers.is(false));
    }

    @Test
    public void testApiCallException_StatusAndThrowableVerbose() {
        ApiCallException thisException = AipCallExceptionHelper.getThrowableApiCallException(true, 201, new UploadException("Bla bla"));
        assertThat(thisException instanceof ApiCallException, is(true));
    }

    @Test
    public void testApiCallException_ThrowableVerbose() {
        ApiCallException thisException = AipCallExceptionHelper.getThrowableApiCallException(true, new ApiKeyMissingException("No Password or API Key provided to log in to AIP Console."));
        assertThat(thisException instanceof ApiCallException, is(true));
        assertThat(thisException.getStackTrace().length, greaterThan(1));
        assertThat(StringUtils.isEmpty(thisException.getStackTrace().toString()), Matchers.is(false));
    }

    @Test
    public void testApiCallException_ThrowableNoVerbose() {
        ApiCallException thisException = AipCallExceptionHelper.getThrowableApiCallException(false, new ApiKeyMissingException("No Password or API Key provided to log in to AIP Console."));
        assertThat(thisException instanceof ApiCallNoStackTraceException, is(true));
        assertThat(thisException.getStackTrace().length, is(0));
    }

    @Test
    public void testApiCallException_StatusAndMessageNoVerbose() {
        ApiCallException thisException = AipCallExceptionHelper.getThrowableApiCallException(false, 201, "Unable to login to AIP Console");
        assertThat(thisException instanceof ApiCallNoStackTraceException, is(true));
        assertThat(thisException.getStackTrace().length, is(0));
    }

    @Test
    public void testApiCallException_StatusNoVerbose() {
        ApiCallException thisException = AipCallExceptionHelper.getThrowableApiCallException(false, 201);
        assertThat(thisException instanceof ApiCallNoStackTraceException, is(true));
        assertThat(thisException.getStackTrace().length, is(0));
    }

    @Test
    public void testApiCallException_StatusAndMessageAndThrowableNoVerbose() {
        ApiCallException thisException = AipCallExceptionHelper.getThrowableApiCallException(false, 201, "Unable to login to AIP Console", new UploadException("Bla bla"));
        assertThat(thisException instanceof ApiCallNoStackTraceException, is(true));
        assertThat(thisException.getStackTrace().length, is(0));
    }

    @Test
    public void testApiCallException_StatusAndThrowableNoVerbose() {
        ApiCallException thisException = AipCallExceptionHelper.getThrowableApiCallException(false, 201, new UploadException("Bla bla"));
        assertThat(thisException instanceof ApiCallNoStackTraceException, is(true));
        assertThat(thisException.getStackTrace().length, is(0));
    }
}
