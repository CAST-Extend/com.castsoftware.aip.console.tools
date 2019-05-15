package com.castsoftware.uc.aip.console.tools;

import com.castsoftware.uc.aip.console.tools.core.services.ChunkedUploadService;
import com.castsoftware.uc.aip.console.tools.core.services.ChunkedUploadServiceImpl;
import com.castsoftware.uc.aip.console.tools.core.services.JobsService;
import com.castsoftware.uc.aip.console.tools.core.services.JobsServiceImpl;
import com.castsoftware.uc.aip.console.tools.core.services.RestApiService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
@ComponentScan
public class ApiIntegrationConfig {
    // 10s connect timeout
    private static final int CONNECT_TIMEOUT = 10_000;
    // 30s read timeout
    private static final int READ_TIMEOUT = 30_000;

    @Bean
    public BasicCookieStore getCookieStore() {
        return new BasicCookieStore();
    }

    @Bean
    public ClientHttpRequestFactory getClientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory();

        CloseableHttpClient client = HttpClients.custom()
                .setDefaultCookieStore(getCookieStore())
                .useSystemProperties()
                .build();

        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        factory.setHttpClient(client);

        return factory;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        objectMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return objectMapper;
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate template = new RestTemplate(getClientHttpRequestFactory());

        template.getMessageConverters().add(new MappingJackson2HttpMessageConverter(objectMapper()));
        return template;
    }

    @Bean
    public JobsService jobsService(@Autowired RestApiService restApiService) {
        return new JobsServiceImpl(restApiService);
    }

    @Bean
    public ChunkedUploadService chunkedUploadService(@Autowired RestApiService restApiService) {
        return new ChunkedUploadServiceImpl(restApiService);
    }
}
