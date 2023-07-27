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
import com.castsoftware.aip.console.tools.core.utils.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Component
@CommandLine.Command(
        name = "ResyncApplicationJob",
        mixinStandardHelpOptions = true,
        aliases = {"resync"},
        description = "Sync application on AIP Console"
)
@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResyncApplicationCommand implements Callable<Integer> {

    @Autowired
    private JobsService jobsService;

    @Autowired
    private RestApiService restApiService;

    @Autowired
    private ApplicationService applicationService;

    @CommandLine.Mixin
    private SharedOptions sharedOptions;

    /**
     * options for the upload and job startup
     */
    @CommandLine.Option(names = {"-n", "--app-name"}, paramLabel = "APPLICATION_NAME", description = "The name of the application to resync", required = true)
    private String appName;

    @Override
    public Integer call() {
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

        log.info("Sync application command has triggered with log output = '{}'", sharedOptions.isVerbose());

        try {
            ApplicationDto app = applicationService.getApplicationFromName(appName);

            String appGuid = app.getGuid();

            String jobGuid = jobsService.startResyncApplication(appGuid);

            log.info(String.format("Started job to resync application for %s", appName));
            return jobsService.pollAndWaitForJobFinished(jobGuid, (jobDetails) -> {
                if (jobDetails.getState() != JobState.COMPLETED) {
                    log.error("Resync of the application failed with status '{}'", jobDetails.getState());
                    return jobDetails.getState() == JobState.CANCELED ? Constants.RETURN_JOB_CANCELED : Constants.RETURN_JOB_FAILED;
                }
                log.info("Application '{}' synced successfully:  GUID is '{}'", jobDetails.getAppName(), jobDetails.getAppGuid());
                return Constants.RETURN_OK;
            }, sharedOptions.isVerbose());
        } catch (JobServiceException e) {
            return Constants.RETURN_JOB_FAILED;
        } catch (ApplicationServiceException e) {
            throw new RuntimeException(e);
        }
    }
}

