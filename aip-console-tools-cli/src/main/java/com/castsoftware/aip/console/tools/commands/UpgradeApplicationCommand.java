package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApiKeyMissingException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.services.UploadService;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.core.utils.VersionInformation;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.util.concurrent.TimeUnit;

@Component
@CommandLine.Command(
        name = "UpgradeApplicationJob",
        mixinStandardHelpOptions = true,
        aliases = {"upgrade"},
        description = "Upgrades application on AIP Console"
)
@Slf4j
@Getter
@Setter
public class UpgradeApplicationCommand extends BasicCallable {

    @CommandLine.Mixin
    private SharedOptions sharedOptions;

    /**
     * options for the upload and job startup
     */
    @CommandLine.Option(names = {"-n", "--app-name"}, paramLabel = "APPLICATION_NAME", description = "The name of the application to upgrade", required = true)
    private String appName;

    protected UpgradeApplicationCommand(RestApiService restApiService, JobsService jobsService, UploadService uploadService, ApplicationService applicationService) {
        super(restApiService, jobsService, uploadService, applicationService);
    }

    @Override
    protected Integer processCallCommand() throws Exception {
        try {
            if (sharedOptions.getTimeout() != Constants.DEFAULT_HTTP_TIMEOUT) {
                restApiService.setTimeout(sharedOptions.getTimeout(), TimeUnit.SECONDS);
            }
            restApiService.validateUrlAndKey(sharedOptions.getFullServerRootUrl(), sharedOptions.getUsername(), sharedOptions.getApiKeyValue());
        } catch (ApiKeyMissingException e) {
            return Constants.RETURN_NO_PASSWORD;
        } catch (ApiCallException e) {
            return Constants.RETURN_LOGIN_ERROR;
        }

        log.info("Upgrade application command has triggered with log output = '{}'", sharedOptions.isVerbose());

        try {
            ApplicationDto app = applicationService.getApplicationFromName(appName);
            String nodeCaipVersion = applicationService.getAipConsoleApiInfo().getCaipVersion();

            String appGuid = app.getGuid();
            String appCaipVersion = app.getCaipVersion();

            log.info(String.format("Caip version of app: %s" , appCaipVersion));
            log.info(String.format("Caip version of node: %s", nodeCaipVersion));

            String jobGuid = jobsService.startUpgradeApplication(appGuid, appName, appCaipVersion, nodeCaipVersion);

            log.info(String.format("Started job to upgrade application for %s", appName));
            return jobsService.pollAndWaitForJobFinished(jobGuid, (jobDetails) -> {
                if (jobDetails.getState() != JobState.COMPLETED) {
                    log.error("Upgrade of the application failed with status '{}'", jobDetails.getState());
                    return jobDetails.getState() == JobState.CANCELED ? Constants.RETURN_JOB_CANCELED : Constants.RETURN_JOB_FAILED;
                }
                log.info("Application '{}' upgraded successfully:  GUID is '{}'", jobDetails.getAppName(), jobDetails.getAppGuid());
                return Constants.RETURN_OK;
            }, sharedOptions.isVerbose());
        } catch (JobServiceException e) {
            return Constants.RETURN_JOB_FAILED;
        } catch (ApplicationServiceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected VersionInformation getMinVersion() {
        return null;
    }

    @Override
    public SharedOptions getSharedOptions() {
        return sharedOptions;
    }
}

