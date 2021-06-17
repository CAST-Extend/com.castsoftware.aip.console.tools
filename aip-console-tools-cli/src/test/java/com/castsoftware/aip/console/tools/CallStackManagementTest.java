package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallNoStackTraceException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.PackagePathInvalidException;
import com.castsoftware.aip.console.tools.core.exceptions.UploadException;
import com.castsoftware.aip.console.tools.core.services.RestApiServiceImpl;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CallStackManagementTest {
    @Test
    public void testAddVersionCommand_loginFailedWithVerbose() throws ApiCallException, ApplicationServiceException, UploadException, JobServiceException, PackagePathInvalidException, IOException {
        OkHttpClient client = Mockito.mock(OkHttpClient.class);
        Call realCall = Mockito.mock(Call.class);
        final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        String bodyJson = "{\n" +
                "    \"error\": \"Unauthorized\",\n" +
                "    \"message\": \"fake Unauthorized user message\",\n" +
                "    \"path\": \"/api/applications/61bc26f5-365c-40b6-b821-29338d879f08/debug-options\",\n" +
                "    \"status\": 401,\n" +
                "    \"timestamp\": \"2021-05-31T13:47:33.089Z\"\n" +
                "}";
        RequestBody body = RequestBody.create(JSON, bodyJson);
        Request request = new Request.Builder()
                .url("https://demo-eu.castsoftware.com/ui/index.html#/")
                .post(body)
                .build();

        Response.Builder responseBuilder = new Response.Builder();
        Response response = responseBuilder.request(request).protocol(Protocol.HTTP_1_0).code(401)
                .message(body.toString())
                .body(ResponseBody.create(JSON, bodyJson))
                .build();
        when(realCall.execute()).thenReturn(response);
        when(client.newCall(any(Request.class))).thenReturn(realCall);
        RestApiServiceImpl restApiService = new RestApiServiceImpl();
        ReflectionTestUtils.setField(restApiService, "client", client);
        ReflectionTestUtils.setField(restApiService, "serverUrl", "https://demo-eu.castsoftware.com");
        ReflectionTestUtils.setField(restApiService, "verbose", true);

        try {
            restApiService.login();
            fail();
        } catch (ApiCallException e) {
            //Mage sure stack trace is there
            assertThat(e.getStackTrace().length, greaterThan(1));
            assertThat(StringUtils.isEmpty(Arrays.stream(e.getStackTrace()).map(StackTraceElement::getClassName).collect(Collectors.joining(";"))), is(false));
        }
    }

    @Test
    public void testAddVersionCommand_loginFailedWithoutVerbose() throws ApiCallException, ApplicationServiceException, UploadException, JobServiceException, PackagePathInvalidException, IOException {
        OkHttpClient client = Mockito.mock(OkHttpClient.class);
        Call realCall = Mockito.mock(Call.class);
        final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        String bodyJson = "{\n" +
                "    \"error\": \"Unauthorized\",\n" +
                "    \"message\": \"fake Unauthorized user message\",\n" +
                "    \"path\": \"/api/applications/61bc26f5-365c-40b6-b821-29338d879f08/debug-options\",\n" +
                "    \"status\": 401,\n" +
                "    \"timestamp\": \"2021-05-31T13:47:33.089Z\"\n" +
                "}";
        RequestBody body = RequestBody.create(JSON, bodyJson);
        Request request = new Request.Builder()
                .url("https://demo-eu.castsoftware.com/ui/index.html#/")
                .post(body)
                .build();

        Response.Builder responseBuilder = new Response.Builder();
        Response response = responseBuilder.request(request).protocol(Protocol.HTTP_1_0).code(401)
                .message(body.toString())
                .body(ResponseBody.create(JSON, bodyJson))
                .build();
        when(realCall.execute()).thenReturn(response);
        when(client.newCall(any(Request.class))).thenReturn(realCall);
        RestApiServiceImpl restApiService = new RestApiServiceImpl();
        ReflectionTestUtils.setField(restApiService, "client", client);
        ReflectionTestUtils.setField(restApiService, "serverUrl", "https://demo-eu.castsoftware.com");
        ReflectionTestUtils.setField(restApiService, "verbose", false);

        try {
            restApiService.login();
            fail();
        } catch (ApiCallException e) {
            //Mage sure stack trace is not there
            Assert.assertThat(e instanceof ApiCallNoStackTraceException, Is.is(true));
            assertThat(e.getStackTrace().length, is(0));
            assertThat(StringUtils.isEmpty(Arrays.stream(e.getStackTrace()).map(StackTraceElement::getClassName).collect(Collectors.joining(";"))), is(true));
        }
    }
}
