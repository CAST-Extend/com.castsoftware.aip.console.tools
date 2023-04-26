package com.castsoftware.aip.console.tools.core.services;

import com.castsoftware.aip.console.tools.core.dto.architecturestudio.ArchitectureModelDto;
import com.castsoftware.aip.console.tools.core.dto.architecturestudio.ArchitectureModelLinkDto;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.utils.ApiEndpointHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.java.Log;

import java.util.Set;

@Log
public class ArchitectureStudioServiceImpl implements ArchitectureStudioService {

    private RestApiService restApiService;

    public ArchitectureStudioServiceImpl(RestApiService restApiService) {
        this.restApiService = restApiService;
    }

    public Set<ArchitectureModelDto> getArchitectureModels() throws ApplicationServiceException {
        try {
            Set<ArchitectureModelDto> result = restApiService.getForEntity(ApiEndpointHelper.getArchitectureModelUrl(), new TypeReference<Set<ArchitectureModelDto>>() {
            });
            return result;
        } catch (ApiCallException e) {
            throw new ApplicationServiceException("Unable to get architecture models", e);
        }
    }

//    public Set<ArchitectureModelDto> getModelCheckUrl(String appGuid, String path) throws ApplicationServiceException {
//        Set<ArchitectureModelLinkDto> modelLinkDtos = restApiService.postForEntity(ApiEndpointHelper.getModelCheckUrl(appGuid), , new TypeReference<Set<ArchitectureModelLinkDto>>() {
//        });
//
//    }
}
