package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.VersionDto;
import com.castsoftware.aip.console.tools.core.dto.VersionStatus;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobRequestBuilder;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobStatusWithSteps;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobType;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApiKeyMissingException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.util.Comparator;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Run analysis for an application and a version on AIP Console
 */
@Component
@CommandLine.Command(
        name = "Analysis",
        mixinStandardHelpOptions = true,
        aliases = {"analyze"},
        description = "Analyses an existing version on AIP Console"
)
@Slf4j
@Getter
@Setter
public class AnalyseCommand implements Callable<Integer> {
    private final RestApiService restApiService;
    private final JobsService jobsService;
    private final ApplicationService applicationService;
    @CommandLine.Mixin
    private SharedOptions sharedOptions;

    @CommandLine.Option(names = {"-n", "--app-name"}, paramLabel = "APPLICATION_NAME", description = "The Name of the application to analyze")
    private String applicationName;
    @CommandLine.Option(names = {"-v", "--version-name"}, paramLabel = "VERSION_NAME", description = "The name of the version to analyze. If omitted, the lastest version will be used.")
    private String versionName;
    @CommandLine.Option(names = {"-d", "--deploy"}, description = "Whether the version should be deployed and used for the analysis")
    private boolean autoDeploy;
    @CommandLine.Option(names = {"-S", "--snapshot"}, description = "Creates a snapshot after running the analysis.")
    private boolean withSnapshot;

    public AnalyseCommand(RestApiService restApiService, JobsService jobsService, ApplicationService applicationService) {
        this.restApiService = restApiService;
        this.jobsService = jobsService;
        this.applicationService = applicationService;
    }

    @Override
    public Integer call() throws Exception {
        if (StringUtils.isBlank(applicationName)) {
            log.error("No application name provided. Exiting.");
            return Constants.RETURN_APPLICATION_INFO_MISSING;
        }

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
        String applicationGuid;

        try {
            log.info("Searching for application '{}' on AIP Console", applicationName);
            applicationGuid = applicationService.getApplicationGuidFromName(applicationName);
            if (StringUtils.isBlank(applicationGuid)) {
                log.error("Application '{}' was not found on AIP Console", applicationName);
                return Constants.RETURN_APPLICATION_NOT_FOUND;
            }
            Set<VersionDto> versions = applicationService.getApplicationVersion(applicationGuid);
            if (versions.isEmpty()) {
                log.error("No version for the given application. Make sure at least one version has been delivered");
                return Constants.RETURN_APPLICATION_NO_VERSION;
            }

            VersionDto versionToAnalyze;
            // Version with name provided
            if (StringUtils.isNotBlank(versionName)) {
                versionToAnalyze = versions.stream().filter(v -> StringUtils.equalsAnyIgnoreCase(v.getName(), versionName)).findFirst().orElse(null);
            } else {
                int statusOrdinal = autoDeploy ? VersionStatus.DELIVERED.ordinal() : VersionStatus.ACCEPTED.ordinal();
                versionToAnalyze = versions
                        .stream()
                        .filter(v -> v.getStatus().ordinal() >= statusOrdinal)
                        .max(Comparator.comparing(VersionDto::getVersionDate)).orElse(null);
            }
            if (versionToAnalyze == null) {
                String message = StringUtils.isBlank(versionName) ?
                        "Couldn't find a version to analyze. Make sure you have an accepted version OR a delivered version and pass the '--auto-deploy' parameter" :
                        "No version with name '" + versionName + "' could be found for application " + applicationName;
                log.error(message);
                return Constants.RETURN_APPLICATION_VERSION_NOT_FOUND;
            }
            log.info("Starting analysis job for version {}", versionToAnalyze.getName());
            // Deploy if auto deploy is true AND version to analyze has status DELIVERED (otherwise just do analysis)
            boolean deployFirst = autoDeploy && versionToAnalyze.getStatus() == VersionStatus.DELIVERED;

            JobRequestBuilder builder = JobRequestBuilder.newInstance(applicationGuid, null, JobType.ANALYZE)
                    .startStep(deployFirst ? Constants.SET_CURRENT_STEP_NAME : Constants.ANALYZE)
                    .endStep(withSnapshot ? Constants.UPLOAD_APP_SNAPSHOT : Constants.ANALYZE)
                    .versionName(versionToAnalyze.getName())
                    .versionGuid(versionToAnalyze.getGuid())
                    .releaseAndSnapshotDate(new Date());
            log.info("Starting job with parameters : \n{}", builder.buildJobRequest());

            String jobGuid = jobsService.startJob(builder);
            JobStatusWithSteps jobStatus = jobsService.pollAndWaitForJobFinished(jobGuid, Function.identity());
            if (JobState.COMPLETED == jobStatus.getState()) {
                log.info("Job completed successfully.");
                return Constants.RETURN_OK;
            }

            log.error("Job did not complete. Status is '{}' on step '{}'", jobStatus.getState(), jobStatus.getFailureStep());
            return Constants.RETURN_JOB_FAILED;
        } catch (ApplicationServiceException e) {
            return Constants.RETURN_APPLICATION_INFO_MISSING;
        } catch (JobServiceException e) {
            return Constants.RETURN_JOB_POLL_ERROR;
        }
    }

}
