package com.castsoftware.uc.aip.console.tools.core.services;

import com.castsoftware.uc.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.uc.aip.console.tools.core.dto.Applications;
import com.castsoftware.uc.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.uc.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.uc.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.uc.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.uc.aip.console.tools.core.utils.ApiEndpointHelper;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

@Log
public class ApplicationServiceImpl implements ApplicationService {

    private RestApiService restApiService;
    private JobsService jobService;

    public ApplicationServiceImpl(RestApiService restApiService, JobsService jobsService) {
        this.restApiService = restApiService;
        this.jobService = jobsService;
    }

    @Override
    public String getApplicationGuidFromName(String applicationName) throws ApplicationServiceException {
        return getApplications()
                .getApplications()
                .stream()
                .filter(Objects::nonNull)
                .filter(a -> StringUtils.equalsAnyIgnoreCase(applicationName, a.getName()))
                .findFirst()
                .map(ApplicationDto::getGuid)
                .orElse(null);
    }

    @Override
    public String getOrCreateApplicationFromName(String applicationName, boolean autoCreate) throws ApplicationServiceException {
        if (StringUtils.isBlank(applicationName)) {
            throw new ApplicationServiceException("No application name provided.");
        }

        Optional<ApplicationDto> appDto = getApplications()
                .getApplications()
                .stream()
                .filter(Objects::nonNull)
                .filter(a -> StringUtils.equalsAnyIgnoreCase(applicationName, a.getName()))
                .findFirst();

        if (!appDto.isPresent()) {
            if (!autoCreate) {
                return null;
            }
            try {
                log.info("Application not found and 'auto create' enabled. Starting application creation");
                String jobGuid = jobService.startCreateApplication(applicationName);
                return jobService.pollAndWaitForJobFinished(jobGuid, (s) -> s.getState() == JobState.COMPLETED ? s.getAppGuid() : null);
            } catch (JobServiceException e) {
                log.log(Level.SEVERE, "Could not create the application due to the following error", e);
                throw new ApplicationServiceException("Unable to create application automatically.", e);
            }
        }
        return appDto.get().getGuid();
    }

    private Applications getApplications() throws ApplicationServiceException {
        try {
            Applications result = restApiService.getForEntity(ApiEndpointHelper.getApplicationsPath(), Applications.class);
            return result == null ? new Applications() : result;
        } catch (ApiCallException e) {
            throw new ApplicationServiceException("Unable to get applications from AIP Console", e);
        }
    }
}
