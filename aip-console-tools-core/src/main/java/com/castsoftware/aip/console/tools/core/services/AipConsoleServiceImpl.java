package com.castsoftware.aip.console.tools.core.services;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.ModuleGenerationType;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobRequestBuilder;
import com.castsoftware.aip.console.tools.core.exceptions.AipConsoleException;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.utils.ApiEndpointHelper;
import lombok.extern.java.Log;

@Log
public class AipConsoleServiceImpl implements AipConsoleService {

    RestApiService apiService;
    ApplicationService applicationService;

    public AipConsoleServiceImpl(RestApiService restApiService, ApplicationService applicationService) {
        this.apiService = restApiService;
        this.applicationService = applicationService;
    }

    @Override
    public ApiInfoDto getAipConsoleApiInfo() throws AipConsoleException {
        try {
            return apiService.getForEntity(ApiEndpointHelper.getRootPath(), ApiInfoDto.class);
        } catch (ApiCallException e) {
            throw new AipConsoleException("Error while retrieving AIP Console capabilities", e);
        }
    }

    @Override
    public void updateModuleGenerationType(String applicationGuid, JobRequestBuilder builder, ModuleGenerationType moduleGenerationType, boolean firstVersion) {
        if (moduleGenerationType != null) {
            if (moduleGenerationType == ModuleGenerationType.FULL_CONTENT) {
                applicationService.setModuleOptionsGenerationType(applicationGuid, moduleGenerationType);
                log.info("Module option has been set to " + moduleGenerationType);
            } else if (firstVersion) {
                //Job will handle it
                builder.moduleGenerationType(moduleGenerationType);
            } else { //clone
                if (moduleGenerationType == ModuleGenerationType.ONE_PER_AU) {
                    applicationService.setModuleOptionsGenerationType(applicationGuid, moduleGenerationType);
                    log.info("Module option has been set to " + moduleGenerationType);
                } else {
                    //delegated to the job that will issue the appropriate message in case of;
                    builder.moduleGenerationType(moduleGenerationType);
                }
            }
        }
    }
}
