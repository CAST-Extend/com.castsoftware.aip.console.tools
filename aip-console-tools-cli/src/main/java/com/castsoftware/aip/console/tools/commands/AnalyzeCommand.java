package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
public class AnalyzeCommand implements Callable<Integer> {
    private static final DateFormat RELEASE_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Autowired
    private RestApiService restApiService;
    @Autowired
    private JobsService jobsService;
    @Autowired
    private ApplicationService applicationService;

    @CommandLine.Mixin
    private SharedOptions sharedOptions;

    @CommandLine.Option(names = {"-n", "--app-name"},
            paramLabel = "APPLICATION_NAME",
            description = "The Name of the application to analyze",
            required = true)
    private String applicationName;

    @CommandLine.Option(names = {"-v", "--version-name"},
            paramLabel = "VERSION_NAME",
            description = "The name of the version to analyze. If omitted, the latest version will be used.")
    private String versionName;

    @CommandLine.Option(names = {"-S", "--snapshot"},
            description = "Creates a snapshot after running the analysis." + " if specified without parameter: ${FALLBACK-VALUE}",
            fallbackValue = "true")
    private boolean withSnapshot;

    @CommandLine.Option(names = "--process-imaging", description = "If provided, will upload data to Imaging. Note: Parameter will be ignored if snapshot option is not provided and Imaging is not setup in AIP Console"
            + " if specified without parameter: ${FALLBACK-VALUE}", fallbackValue = "true")
    private boolean processImaging = false;
    
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
        ApiInfoDto apiInfoDto = restApiService.getAipConsoleApiInfo();

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
                versionToAnalyze = versions
                        .stream()
                        .filter(v -> v.getStatus().ordinal() >= VersionStatus.DELIVERED.ordinal())
                        .max(Comparator.comparing(VersionDto::getVersionDate)).orElse(null);
            }
            if (versionToAnalyze == null) {
                String message = StringUtils.isBlank(versionName) ?
                        "Couldn't find a version to analyze. Make sure you have an accepted version OR a delivered version and pass the '--auto-deploy' parameter" :
                        "No version with name '" + versionName + "' could be found for application " + applicationName;
                log.error(message);
                return Constants.RETURN_APPLICATION_VERSION_NOT_FOUND;
            }
            // Deploy if auto deploy is true AND version to analyze has status DELIVERED (otherwise just do analysis)
            boolean deployFirst = versionToAnalyze.getStatus() == VersionStatus.DELIVERED;

            JobRequestBuilder builder = JobRequestBuilder.newInstance(applicationGuid, null, JobType.ANALYZE)
                    .startStep(deployFirst ? Constants.ACCEPTANCE_STEP_NAME : Constants.ANALYZE);

            if (withSnapshot) {
                builder.processImaging(processImaging)
                        .snapshotName(String.format("Snapshot-%s", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date())))
                        .uploadApplication(true);
            } else {
                builder.endStep(Constants.ANALYZE);
            }

            builder.versionName(versionToAnalyze.getName())
                    .versionGuid(versionToAnalyze.getGuid())
                    .releaseAndSnapshotDate(new Date());

            log.info("Running analysis for application '{}' with version '{}'", applicationName, versionToAnalyze.getName());
            String jobGuid = jobsService.startJob(builder);
            Thread shutdownHook = getShutdownHookForJobGuid(jobGuid);

            Runtime.getRuntime().addShutdownHook(shutdownHook);
            JobStatusWithSteps jobStatus = jobsService.pollAndWaitForJobFinished(jobGuid, Function.identity(), sharedOptions.isVerbose());
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            if (JobState.COMPLETED == jobStatus.getState()) {
                log.info("Application Analysis completed successfully");
                return Constants.RETURN_OK;
            }

            log.error("Analysis did not complete. Status is '{}' on step '{}'", jobStatus.getState(), jobStatus.getFailureStep());
            return Constants.RETURN_JOB_FAILED;
        } catch (ApplicationServiceException e) {
            return Constants.RETURN_APPLICATION_INFO_MISSING;
        } catch (JobServiceException e) {
            return Constants.RETURN_JOB_POLL_ERROR;
        }
    }

    private Thread getShutdownHookForJobGuid(String jobGuid) {
        return new Thread(() -> {
            log.info("Received termination signal. Cancelling currently running job on AIP Console and exiting.");
            try {
                jobsService.cancelJob(jobGuid);
            } catch (JobServiceException e) {
                log.error("Cannot cancel the job on AIP Console. Please cancel it manually.", e);
            }
        });
    }
}
