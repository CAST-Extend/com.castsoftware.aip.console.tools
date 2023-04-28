package com.castsoftware.aip.console.tools.core.services;

import com.castsoftware.aip.console.tools.core.dto.architecturestudio.ArchitectureModelDto;
import com.castsoftware.aip.console.tools.core.dto.architecturestudio.ArchitectureModelLinkDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.PathRequest;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.utils.ApiEndpointHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.java.Log;

import java.util.Set;

@Log
public class ArchitectureStudioServiceImpl implements ArchitectureStudioService {

    private final RestApiService restApiService;

    public ArchitectureStudioServiceImpl(RestApiService restApiService) {
        this.restApiService = restApiService;
    }

    public Set<ArchitectureModelDto> getArchitectureModels() throws ApplicationServiceException {
        try {
            String modelUrl = ApiEndpointHelper.getArchitectureModelUrl();
            return restApiService.getForEntity(modelUrl, new TypeReference<Set<ArchitectureModelDto>>() {
            });
        } catch (ApiCallException e) {
            throw new ApplicationServiceException("Unable to get architecture models", e);
        }
    }

    public Set<ArchitectureModelLinkDto> modelChecker(String appGuid, String path, String caipVersion) throws ApiCallException {

        PathRequest pathRequest = PathRequest.builder().path(path).build();
        return restApiService.postForEntity(
                ApiEndpointHelper.getModelCheckUrl(appGuid),
                pathRequest,
                new TypeReference<Set<ArchitectureModelLinkDto>>(){}
        );
    }
}
