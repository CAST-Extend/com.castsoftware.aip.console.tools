package com.castsoftware.aip.console.tools.core.services;

import com.castsoftware.aip.console.tools.core.dto.architecturestudio.ArchitectureModelDto;
import com.castsoftware.aip.console.tools.core.dto.architecturestudio.ArchitectureModelLinkDto;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;

import java.util.Set;


public interface ArchitectureStudioService {

    Set<ArchitectureModelDto> getArchitectureModels() throws ApplicationServiceException;

    Set<ArchitectureModelLinkDto>  modelChecker(String appGuid, String path, String caipVersion) throws ApiCallException;

    void downloadCheckedModelReport(String appGuid, String modelName, Integer metricId, String description, Integer transactionId, Set<ArchitectureModelLinkDto> modelChecker, String reportPath) throws Exception;

}
