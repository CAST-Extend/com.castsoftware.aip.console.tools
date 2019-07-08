package com.castsoftware.uc.aip.console.tools.commands;

import com.castsoftware.uc.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.uc.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.uc.aip.console.tools.core.exceptions.ApiKeyMissingException;
import com.castsoftware.uc.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.uc.aip.console.tools.core.services.JobsService;
import com.castsoftware.uc.aip.console.tools.core.services.RestApiService;
import com.castsoftware.uc.aip.console.tools.core.utils.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Component
@CommandLine.Command(
        name = "CreateApplication",
        mixinStandardHelpOptions = true,
        aliases = {"new"},
        description = "Creates a new application on AIP Console"
)
@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateApplicationCommand implements Callable<Integer> {

    @Autowired
    private JobsService jobsService;

    @Autowired
    private RestApiService restApiService;

    @CommandLine.Mixin
    private SharedOptions sharedOptions;

    /**
     * options for the upload and job startup
     */
    @CommandLine.Option(names = {"-n", "--app-name"}, paramLabel = "APPLICATION_NAME", description = "The name of the application to create", required = true)
    private String applicationName;

    @CommandLine.Unmatched
    private List<String> unmatchedOptions;

    @Override
    public Integer call() {
        try {
            restApiService.setTimeout(sharedOptions.getTimeout(), TimeUnit.SECONDS);
            restApiService.validateUrlAndKey(sharedOptions.getFullServerRootUrl(), sharedOptions.getUsername(), sharedOptions.getApiKeyValue());
        } catch (ApiKeyMissingException e) {
            return Constants.RETURN_NO_PASSWORD;
        } catch (ApiCallException e) {
            return Constants.RETURN_LOGIN_ERROR;
        }

        try {
            String jobGuid = jobsService.startCreateApplication(applicationName);
            log.info("Started job to create new application.");
            return jobsService.pollAndWaitForJobFinished(jobGuid, (jobDetails) -> {
                if (jobDetails.getState() != JobState.COMPLETED) {
                    log.error("Creation of version failed with status '{}'", jobDetails.getState());
                    return Constants.RETURN_JOB_FAILED;
                }
                log.info("Creation of version successful for application '{}'. Application GUID is '{}'", jobDetails.getAppName(), jobDetails.getAppGuid());
                return Constants.RETURN_OK;
            });
        } catch (JobServiceException e) {
            return Constants.RETURN_JOB_FAILED;
        }
    }


}
