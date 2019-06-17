package com.castsoftware.uc.aip.console.tools;

import com.castsoftware.uc.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.uc.aip.console.tools.core.services.ApplicationServiceImpl;
import com.castsoftware.uc.aip.console.tools.core.services.ChunkedUploadService;
import com.castsoftware.uc.aip.console.tools.core.services.ChunkedUploadServiceImpl;
import com.castsoftware.uc.aip.console.tools.core.services.JobsService;
import com.castsoftware.uc.aip.console.tools.core.services.JobsServiceImpl;
import com.castsoftware.uc.aip.console.tools.core.services.RestApiService;
import com.castsoftware.uc.aip.console.tools.core.services.RestApiServiceImpl;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.http.impl.client.BasicCookieStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

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
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        objectMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return objectMapper;
    }

    @Bean
    public RestApiService restApiService(@Autowired ObjectMapper objectMapper) {
        return new RestApiServiceImpl(objectMapper);
    }

    @Bean
    public JobsService jobsService(@Autowired RestApiService restApiService) {
        return new JobsServiceImpl(restApiService);
    }

    @Bean
    public ChunkedUploadService chunkedUploadService(@Autowired RestApiService restApiService) {
        return new ChunkedUploadServiceImpl(restApiService);
    }

    @Bean
    public ApplicationService applicationService(@Autowired RestApiService restApiService, @Autowired JobsService jobsService) {
        return new ApplicationServiceImpl(restApiService, jobsService);
    }
}
