package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.core.services.AipConsoleService;
import com.castsoftware.aip.console.tools.core.services.AipConsoleServiceImpl;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.ApplicationServiceImpl;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.JobsServiceImpl;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.services.RestApiServiceImpl;
import com.castsoftware.aip.console.tools.core.services.UploadService;
import com.castsoftware.aip.console.tools.core.services.UploadServiceImpl;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.impl.client.BasicCookieStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
public class ApiIntegrationConfig {

    @Bean
    public BasicCookieStore getCookieStore() {
        return new BasicCookieStore();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        objectMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
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
    public UploadService chunkedUploadService(@Autowired RestApiService restApiService) {
        return new UploadServiceImpl(restApiService);
    }

    @Bean
    public ApplicationService applicationService(@Autowired RestApiService restApiService, @Autowired JobsService jobsService) {
        return new ApplicationServiceImpl(restApiService, jobsService);
    }

    @Bean
    public AipConsoleService aipConsoleService(@Autowired RestApiService restApiService, @Autowired ApplicationService applicationService) {
        return new AipConsoleServiceImpl(restApiService, applicationService);
    }
}
