package com.castsoftware.aip.console.tools.core.services;

import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApiKeyMissingException;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.extern.java.Log;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Log
public class RestApiServiceImpl implements RestApiService {
    private static final List<Integer> ACCEPTED_HTTP_CODES = Arrays.asList(200, 201, 202, 204);

    private OkHttpClient client;
    private ObjectMapper mapper;
    private QueryableCookieJar cookieJar;
    private String serverUrl;
    private String username;
    private String key;

    public RestApiServiceImpl() {
        this.cookieJar = new QueryableCookieJar();
        this.client = new OkHttpClient.Builder()
                .addInterceptor(getAuthInterceptor())
                .cookieJar(cookieJar)
                .connectTimeout(Constants.DEFAULT_HTTP_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.DEFAULT_HTTP_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(Constants.DEFAULT_HTTP_TIMEOUT, TimeUnit.SECONDS)
                .build();
        this.mapper = new ObjectMapper();
        this.mapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        this.mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        this.mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public RestApiServiceImpl(ObjectMapper preConfiguredMapper) {
        this.cookieJar = new QueryableCookieJar();
        this.client = new OkHttpClient.Builder()
                .addInterceptor(getAuthInterceptor())
                .cookieJar(cookieJar)
                .connectTimeout(Constants.DEFAULT_HTTP_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.DEFAULT_HTTP_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(Constants.DEFAULT_HTTP_TIMEOUT, TimeUnit.SECONDS)
                .build();
        this.mapper = preConfiguredMapper;
    }

    @Override
    public void setTimeout(long timeout, TimeUnit timeUnit) {
        if (timeout < 0) {
            // negative timeout ? don't update the client
            return;
        }
        // Get a new builder from initial client and update timeouts
        this.client = this.client.newBuilder()
                .connectTimeout(timeout, timeUnit)
                .readTimeout(timeout, timeUnit)
                .writeTimeout(timeout, timeUnit)
                .build();
    }

    @Override
    public void validateUrlAndKey(String serverUrl, String apiKey) throws ApiCallException {
        assert StringUtils.isNoneBlank(serverUrl);

        if(StringUtils.isBlank(apiKey)) {
            log.severe("No Password or API Key provided to log in to AIP Console.");
            throw new ApiKeyMissingException("No Password or API Key provided to log in to AIP Console.");
        }

        if (!StringUtils.startsWithIgnoreCase(serverUrl, "http")) {
            serverUrl = "http://" + serverUrl;
        }
        if (StringUtils.endsWithIgnoreCase(serverUrl, "/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }

        this.serverUrl = serverUrl;
        this.key = apiKey;
        login();
    }

    @Override
    public void validateUrlAndKey(String serverUrl, String username, String password) throws ApiCallException {
        this.username = username;
        validateUrlAndKey(serverUrl, password);
    }

    @Override
    public <T> T getForEntity(String endpoint, Class<T> responseClass) throws ApiCallException {
        return exchangeForEntity("GET", endpoint, null, responseClass);
    }

    @Override
    public <T> T getForEntity(String endpoint, TypeReference<T> type) throws ApiCallException {
        return exchangeForEntity("GET", endpoint, null, type);
    }

    @Override
    public <T> T postForEntity(String endpoint, Object entity, Class<T> responseClass) throws ApiCallException {
        return exchangeForEntity("POST", endpoint, entity, responseClass);
    }

    @Override
    public <T> T postForEntity(String endpoint, Object entity, TypeReference<T> type) throws ApiCallException {
        return exchangeForEntity("POST", endpoint, entity, type);
    }

    @Override
    public <T> T patchForEntity(String endpoint, Object entity, Class<T> responseClass) throws ApiCallException {
        return exchangeForEntity("PATCH", endpoint, entity, responseClass);
    }

    @Override
    public <T> T patchForEntity(String endpoint, Object entity, TypeReference<T> type) throws ApiCallException {
        return exchangeForEntity("PATCH", endpoint, entity, type);
    }

    @Override
    public <T> T putForEntity(String endpoint, Object entity, Class<T> responseClass) throws ApiCallException {
        return exchangeForEntity("PUT", endpoint, entity, responseClass);
    }

    @Override
    public <T> T putForEntity(String endpoint, Object entity, TypeReference<T> type) throws ApiCallException {
        return exchangeForEntity("PUT", endpoint, entity, type);
    }

    @Override
    public <T> T deleteForEntity(String endpoint, Object entity, Class<T> responseClass) throws ApiCallException {
        return exchangeForEntity("DELETE", endpoint, entity, responseClass);
    }

    @Override
    public <T> T deleteForEntity(String endpoint, Object entity, TypeReference<T> type) throws ApiCallException {
        return exchangeForEntity("DELETE", endpoint, entity, type);
    }

    @Override
    public <T> T exchangeMultipartForEntity(String method, String endpoint, Map<String, Map<String, String>> headers, Map<String, Object> content, Class<T> responseClass) throws ApiCallException {
        Request.Builder reqBuilder = getRequestBuilder(endpoint);
        log.finer(String.format("Executing MULTIPART call with method %s to endpoint %s", method, endpoint));

        MultipartBody.Builder builder = new MultipartBody.Builder();

        for (Map.Entry<String, Object> entry : content.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            RequestBody body;
            String filename;
            // Add some raw content
            if (value instanceof byte[]) {
                filename = "filechunk";
                body = RequestBody.create(MediaType.parse("application/octet-stream"), (byte[]) value);
            } else {
                filename = null;
                body = getRequestBodyForEntity(value);
            }
            MultipartBody.Part part = MultipartBody.Part.createFormData(key, filename, body);
            builder.addPart(part);
        }

        Request req = reqBuilder.method(method, builder.build())
                .build();

        try (Response response = client.newCall(req).execute()) {
            if (ACCEPTED_HTTP_CODES.contains(response.code())) {
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    try (InputStream bodyStream = responseBody.byteStream()) {
                        return mapper.readValue(bodyStream, responseClass);
                    } catch (MismatchedInputException e) {
                        log.log(Level.WARNING, "Unable to parse object as " + responseClass.getName() + "(expected ?). Returning null instead.", e);
                        throw e;
                    }
                }
            }
            log.log(Level.SEVERE, "Response code from API was unexpected : " + response.code());
            log.log(Level.SEVERE, "Content was " + (response.body() == null ? "EMPTY" : response.body().string()));
            throw new ApiCallException(response.code(), "Unable to execute multipart form data with provided content");
        } catch (IOException e) {
            log.log(Level.SEVERE, "IOException when calling endpoint " + endpoint, e);
            throw new ApiCallException(500, e);
        }
    }

    private <T> T exchangeForEntity(String method, String endpoint, Object entity, Class<T> responseClass) throws ApiCallException {
        return exchangeForEntity(method, endpoint, entity, TypeFactory.defaultInstance().constructType(responseClass));
    }

    private <T> T exchangeForEntity(String method, String endpoint, Object entity, TypeReference<T> typeReference) throws ApiCallException {
        return exchangeForEntity(method, endpoint, entity, TypeFactory.defaultInstance().constructType(typeReference));
    }

    private <T> T exchangeForEntity(String method, String endpoint, Object entity, JavaType javaType) throws ApiCallException {
        Request request = getRequestBuilder(endpoint)
                .method(method, getRequestBodyForEntity(entity))
                .build();
        log.fine(String.format("Executing call with method %s to endpoint %s", method, endpoint));
        log.finest("Entity is " + entity);

        try (Response response = client.newCall(request).execute()) {
            if (ACCEPTED_HTTP_CODES.contains(response.code())) {
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    try (InputStream bodyStream = responseBody.byteStream()) {
                        if (String.class.isAssignableFrom(javaType.getRawClass())) {
                            // may be used for debug purposes
                            return (T) IOUtils.toString(bodyStream, StandardCharsets.UTF_8);
                        } else {
                            return mapper.readValue(bodyStream, javaType);
                        }
                    } catch (MismatchedInputException e) {
                        log.log(Level.WARNING, "Unable to parse object as " + javaType.getRawClass().getName() + "(expected ?). Returning null instead.", e);
                        return null;
                    }
                }
                log.fine("No body in response to parse");
                return null;
            }
            String message = "Response code from API was unexpected : " + response.code();
            message += "\nContent was " + (response.body() == null ? "EMPTY" : response.body().string());
            throw new ApiCallException(response.code(), message);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to send request", e);
            throw new ApiCallException(500, e);
        }
    }

    @Override
    public void login() throws ApiCallException {
        Request request = getRequestBuilder("/api/user")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (ArrayUtils.contains(new int[]{200, 201, 202, 204}, response.code())) {
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    log.finest("Body is " + responseBody.string());
                }
                return;
            }
            log.severe("Login to AIP Console failed (http status is " + response.code() + ")");
            log.severe("Content was " + (response.body() == null ? "EMPTY" : response.body().string()));
            throw new ApiCallException(response.code(), "Unable to login to AIP Console");
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to send request", e);
            throw new ApiCallException(500, e);
        }
    }

    private Request.Builder getRequestBuilder(String endpoint) {
        String url;
        if (StringUtils.startsWithIgnoreCase(endpoint, "/")) {
            url = this.serverUrl + endpoint;
        } else {
            url = this.serverUrl + "/" + endpoint;
        }

        Request.Builder builder = new Request.Builder();
        builder.url(url);

        return builder;
    }

    private RequestBody getRequestBodyForEntity(Object entity) throws ApiCallException {
        if (entity == null) {
            return null;
        }
        try {
            return RequestBody.create(
                    MediaType.parse("application/json"),
                    mapper.writeValueAsString(entity));
        } catch (JsonProcessingException e) {
            log.log(Level.SEVERE, "Unable to map object of type " + entity.getClass().getName() + " to JSON", e);
            throw new ApiCallException(500, e);
        }
    }

    /**
     * Create an interceptor to add authentication headers
     * <p/>
     * It'll also add XSRF Token to the request (to avoid 403s)
     *
     * @return an Interceptor instance that'll add Authentication headers if necessary
     */
    private Interceptor getAuthInterceptor() {
        return new AipLoginInterceptor();
    }

    /**
     * Simple cookie jar impl with ability to query a cookie value
     */
    private static class QueryableCookieJar implements CookieJar {
        private Set<Cookie> cookieSet = new HashSet<>();

        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            cookies.forEach(c -> {
                Optional<Cookie> optCookie = cookieSet.stream()
                        .filter(prevCookie -> StringUtils.equalsIgnoreCase(c.name(), prevCookie.name()))
                        .findFirst();
                optCookie.ifPresent(cookie -> cookieSet.remove(cookie));
                cookieSet.add(c);
            });
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            return new ArrayList<>(cookieSet);
        }

        public Cookie getCookieByName(String name) {
            return cookieSet
                    .stream()
                    .filter(c -> StringUtils.equalsIgnoreCase(c.name(), name))
                    .findFirst()
                    .orElse(null);
        }
    }

    private class AipLoginInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            Cookie xsrfCookie = cookieJar.getCookieByName("XSRF-TOKEN");

            Response response;
            Request.Builder reqBuilder = request.newBuilder();

            // get xsrf cookie
            if (xsrfCookie != null) {
                log.finest("Setting XSRF-TOKEN header to " + xsrfCookie.value());
                reqBuilder.header("X-XSRF-TOKEN", xsrfCookie.value());
            } else {
                log.finest("No xsrf cookie, next request should set it");
            }

            if (request.header("Authorization") != null ||
                    request.header(Constants.API_KEY_HEADER) != null) {
                // authentication already defined
                return chain.proceed(reqBuilder.build());
            } else {
                if (!StringUtils.isBlank(username)) {
                    reqBuilder.header("Authorization", Credentials.basic(username, key));
                } else {
                    reqBuilder.header(Constants.API_KEY_HEADER, key);
                }
                return chain.proceed(reqBuilder.build());
            }
        }
    }
}
