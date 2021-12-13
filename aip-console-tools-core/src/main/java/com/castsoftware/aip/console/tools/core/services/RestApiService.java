package com.castsoftware.aip.console.tools.core.services;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.fasterxml.jackson.core.type.TypeReference;
import okhttp3.Response;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface RestApiService {
    /**
     * Update the OkHttpClient with the given timeout
     *
     * @param timeout  the timeout, between 0 and {@link Integer#MAX_VALUE}
     * @param timeUnit A time unit for the timeout
     */
    void setTimeout(long timeout, TimeUnit timeUnit);

    /**
     * @param serverUrl
     * @param apiKey
     * @throws ApiCallException
     */
    void validateUrlAndKey(String serverUrl, String apiKey) throws ApiCallException;

    /**
     * @param serverUrl
     * @param username
     * @param password
     * @throws ApiCallException
     */
    void validateUrlAndKey(String serverUrl, String username, String password) throws ApiCallException;

    Response exchangeForResponse(String method, String endPoints, Object entity) throws ApiCallException;

    <T> T mapResponse(Response response, Class<T> responseClass);

    <T> T mapResponse(Response response, TypeReference<T> typeReference);

    /**
     * This method authenticates the user with provided credentials in validate Url and Key
     *
     * @throws ApiCallException
     */
    void login() throws ApiCallException;

    /**
     * Retrieve the API Info from AIP Console
     *
     * @return an {@link ApiInfoDto} instance
     */
    ApiInfoDto getAipConsoleApiInfo();

    <T> T getForEntity(String endpoint, Class<T> clazz) throws ApiCallException;

    <T> T getForEntity(String endpoint, TypeReference<T> clazz) throws ApiCallException;

    <T> T postForEntity(String endpoint, Object entity, Class<T> responseClass) throws ApiCallException;

    <T> T postForEntity(String endpoint, Object entity, TypeReference<T> responseClass) throws ApiCallException;

    <T> T patchForEntity(String endpoint, Object entity, Class<T> responseClass) throws ApiCallException;

    <T> T patchForEntity(String endpoint, Object entity, TypeReference<T> responseClass) throws ApiCallException;

    <T> T putForEntity(String endpoint, Object entity, Class<T> responseClass) throws ApiCallException;

    <T> T putForEntity(String endpoint, Object entity, TypeReference<T> responseClass) throws ApiCallException;

    <T> T deleteForEntity(String endpoint, Object entity, Class<T> clazz) throws ApiCallException;

    <T> T deleteForEntity(String endpoint, Object entity, TypeReference<T> clazz) throws ApiCallException;

    /**
     * This method is used for multipart content exchange
     * <p>
     * The headers and content maps should contain the same keys and associated content.
     * Each "key" should be associated with a Part of the multipart form, header and content.
     *
     * @param method   The HttpMethod to use for the exchange
     * @param endpoint The endpoint to call
     * @param headers  a Map containing, as key, the part name and as values, a map of headers
     * @param content  a Map containing, as key, the part namd and as values, the content to add to the request
     * @param clazz    The return type
     * @param <T>      The type of obejct to return
     * @return the response of API with type T
     * @throws ApiCallException
     */
    public <T> T exchangeMultipartForEntity(String method, String endpoint, Map<String, Map<String, String>> headers, Map<String, Object> content, Class<T> clazz) throws ApiCallException;

    public <T> T exchangeMultipartForEntity(String method, String endpoint, Map<String, Map<String, String>> headers, File content, Class<T> clazz) throws ApiCallException;
}
