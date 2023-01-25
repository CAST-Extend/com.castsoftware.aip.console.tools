package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApiKeyMissingException;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.services.UploadService;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.core.utils.VersionInformation;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
@Slf4j
public abstract class BasicCollable implements Callable<Integer> {
    protected final RestApiService restApiService;
    protected final JobsService jobsService;
    protected final UploadService uploadService;
    protected final ApplicationService applicationService;

    protected BasicCollable(RestApiService restApiService, JobsService jobsService
            , UploadService uploadService, ApplicationService applicationService) {
        this.applicationService = applicationService;
        this.jobsService = jobsService;
        this.uploadService = uploadService;
        this.restApiService = restApiService;
    }

    protected BasicCollable(RestApiService restApiService, JobsService jobsService, ApplicationService applicationService) {
        this(restApiService, jobsService, null, applicationService);
    }

    protected abstract Integer processCallCommand() throws Exception;

    /**
     * Used for feature compatibility validation
     *
     * @return Minimum accepted version for feature to work
     */
    protected abstract VersionInformation getMinVersion();

    /**
     * Used for feature compatibility validation. When null then the feature operates from getMinVersion()
     *
     * @return Maximum accepted version for feature to work
     */
    protected VersionInformation getMaxVersion() {
        return null;
    }

    public abstract SharedOptions getSharedOptions();

    @Override
    public Integer call() throws Exception {
        try {
            if (getSharedOptions().getTimeout() != Constants.DEFAULT_HTTP_TIMEOUT) {
                restApiService.setTimeout(getSharedOptions().getTimeout(), TimeUnit.SECONDS);
            }
            restApiService.validateUrlAndKey(getSharedOptions().getFullServerRootUrl(), getSharedOptions().getUsername(), getSharedOptions().getApiKeyValue());
        } catch (ApiKeyMissingException e) {
            return Constants.RETURN_NO_PASSWORD;
        } catch (ApiCallException e) {
            return Constants.RETURN_LOGIN_ERROR;
        }

        ApiInfoDto apiInfoDto = applicationService.getAipConsoleApiInfo();
        String apiVersion = apiInfoDto.getApiVersion();
        if (getMinVersion() != null && StringUtils.isNotEmpty(apiVersion)) {
            VersionInformation serverApiVersion = VersionInformation.fromVersionString(apiVersion);
            if (serverApiVersion != null && getMinVersion().isHigherThan(serverApiVersion)) {
                log.error("This feature {} is not compatible with the CAST Imaging Console version {}. Please upgrade to minimum {} version."
                        , "Onboard Application", apiVersion, getMinVersion().toString());
                return Constants.RETURN_SERVER_VERSION_NOT_COMPATIBLE;
            }
        }

        return processCallCommand();
    }
}
