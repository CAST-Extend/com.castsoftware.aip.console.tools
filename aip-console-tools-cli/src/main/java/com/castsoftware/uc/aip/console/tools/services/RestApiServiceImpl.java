package com.castsoftware.uc.aip.console.tools.services;

import com.castsoftware.uc.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.uc.aip.console.tools.core.exceptions.ApiKeyMissingException;
import com.castsoftware.uc.aip.console.tools.core.services.RestApiService;
import com.castsoftware.uc.aip.console.tools.core.utils.ApiEndpointHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class RestApiServiceImpl implements RestApiService {

    private static final String XSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String XSRF_HEADER_KEY = "X-XSRF-TOKEN";
    private static final String API_KEY_HEADER_NAME = "X-API-KEY";

    private static final HttpStatus[] ACCEPTED_STATUSES = new HttpStatus[]{HttpStatus.OK, HttpStatus.CREATED, HttpStatus.ACCEPTED, HttpStatus.NO_CONTENT};

    private String aipConsoleRootUrl = "http://localhost:8081";
    private String username;
    private String apiKey;
    private boolean initialized = false;

    private RestTemplate restTemplate;
    private BasicCookieStore cookieStore;

    public RestApiServiceImpl(@Autowired RestTemplate restTemplate, @Autowired BasicCookieStore cookieStore) {
        this.restTemplate = restTemplate;
        this.cookieStore = cookieStore;
    }

    @Override
    public void validateUrlAndKey(String serverUrl, String apiKey) throws ApiCallException {
        this.aipConsoleRootUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 2) : serverUrl;
        this.apiKey = apiKey;

        if (StringUtils.isBlank(apiKey)) {
            log.error("No Api Key provided.");
            throw new ApiKeyMissingException("No Api key provided.");
        }

        // Login
        try {
            log.debug("Checking access to AIP with given api key");
            login();
        } catch (ApiCallException e) {
            log.error("Could not log in to AIP Console. Please check your Api Key and availability of AIP Console at '{}'", aipConsoleRootUrl);
            throw e;
        }
        initialized = true;
    }

    @Override
    public void validateUrlAndKey(String serverUrl, String username, String password) throws ApiCallException {
        this.username = username;
        validateUrlAndKey(serverUrl, password);
    }

    @Override
    public <T> T getForEntity(String endpoint, Class<T> clazz) throws ApiCallException {
        if (!initialized) {
            throw new ApiCallException("Not logged in to AIP Console. Cannot perform requested action");
        }
        return exchangeBodyForEntityWithLogin(HttpMethod.GET, endpoint, null, clazz);
    }

    @Override
    public <T> T postForEntity(String endpoint, Object entity, Class<T> responseClass) throws ApiCallException {
        if (!initialized) {
            throw new ApiCallException("Not logged in to AIP Console. Cannot perform requested action");
        }
        return exchangeBodyForEntityWithLogin(HttpMethod.POST, endpoint, entity, responseClass);
    }

    @Override
    public <T> T patchForEntity(String endpoint, Object entity, Class<T> responseClass) throws ApiCallException {
        if (!initialized) {
            throw new ApiCallException("Not logged in to AIP Console. Cannot perform requested action");
        }
        return exchangeBodyForEntityWithLogin(HttpMethod.PATCH, endpoint, entity, responseClass);
    }

    @Override
    public <T> T putForEntity(String endpoint, Object entity, Class<T> responseClass) throws ApiCallException {
        if (!initialized) {
            throw new ApiCallException("Not logged in to AIP Console. Cannot perform requested action");
        }
        return exchangeBodyForEntityWithLogin(HttpMethod.PUT, endpoint, entity, responseClass);
    }

    @Override
    public <T> T deleteForEntity(String endpoint, Object entity, Class<T> clazz) throws ApiCallException {
        if (!initialized) {
            throw new ApiCallException("Not logged in to AIP Console. Cannot perform requested action");
        }
        return exchangeBodyForEntityWithLogin(HttpMethod.DELETE, endpoint, entity, clazz);
    }

    @Override
    public <T> T exchangeMultipartForEntity(String method, String endpoint, Map<String, Map<String, String>> headers, Map<String, Object> content, Class<T> clazz) throws ApiCallException {
        assert headers.entrySet().containsAll(content.entrySet());

        MultiValueMap<String, Object> multipartForm = new LinkedMultiValueMap<>();
        Map<String, MultiValueMap<String, String>> headersMap = new HashMap<>();
        headers.forEach((k, v) -> {
            MultiValueMap<String, String> partHeader = new LinkedMultiValueMap<>();
            v.forEach((headerName, headerValue) -> {
                List<String> headerValueList = new ArrayList<>();
                headerValueList.add(headerValue);
                partHeader.put(headerName, headerValueList);
            });

            headersMap.put(k, partHeader);
        });

        content.forEach((k, v) -> {
            MultiValueMap<String, String> contentHeaders = headersMap.get(k);
            HttpEntity partEntity = new HttpEntity(v, contentHeaders);
            List<Object> entityList = new ArrayList<>();
            entityList.add(partEntity);
            multipartForm.put(k, entityList);
        });

        return exchangeBodyForEntityWithLogin(HttpMethod.valueOf(method), endpoint, multipartForm, clazz);
    }

    private <T> T exchangeBodyForEntityWithLogin(HttpMethod method, String endpoint, Object body, Class<T> responseClass) throws ApiCallException {
        login();

        return exchangeBodyForEntity(method, endpoint, body, responseClass);
    }

    private <T> T exchangeBodyForEntity(HttpMethod method, String endpoint, Object body, Class<T> responseClass) throws ApiCallException {

        try {
            String url = aipConsoleRootUrl + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);
            HttpEntity<Object> request = getRequestEntity(body);
            ResponseEntity<T> response = restTemplate.exchange(url, method, request, responseClass);
            if (!ArrayUtils.contains(ACCEPTED_STATUSES, response.getStatusCode())) {
                log.error("Invalid response from API. Status " + response.getStatusCode());
                log.error("Returned content from API is : \n" + response.getBody());
                throw new ApiCallException("Invalid response code from API.");
            }
            return response.getBody();
        } catch (RestClientResponseException e) {
            log.error("Invalid response from API at " + endpoint + ". Status " + e.getRawStatusCode() + ". Content is :\n" + e.getResponseBodyAsString());
            throw new ApiCallException(e);
        } catch (RestClientException e) {
            log.error("Error occurred while trying to call API at " + endpoint, e);
            throw new ApiCallException(e);
        }
    }

    private HttpEntity<Object> getRequestEntity(Object body) {
        HttpHeaders headers = new HttpHeaders();
        Optional<Cookie> xsrfToken = cookieStore.getCookies().stream().filter(c -> XSRF_COOKIE_NAME.equalsIgnoreCase(c.getName())).findFirst();
        xsrfToken.ifPresent(cookie -> headers.set(XSRF_HEADER_KEY, cookie.getValue()));
        // TODO: Remove basic authentication in a future version
        if (StringUtils.isNotBlank(username)) {
            headers.setBasicAuth(username, apiKey, StandardCharsets.UTF_8);
        } else {
            headers.set(API_KEY_HEADER_NAME, apiKey);
        }
        return new HttpEntity<>(body, headers);
    }

    /**
     * This methods logs in the user
     * If the user was already logged in previously, then the session will be refreshed
     *
     * @throws ApiCallException If we are unable to login on the provided server with the given credentials
     */
    private void login() throws ApiCallException {
        exchangeBodyForEntity(HttpMethod.GET, ApiEndpointHelper.getLoginPath(), null, String.class);
    }
}
