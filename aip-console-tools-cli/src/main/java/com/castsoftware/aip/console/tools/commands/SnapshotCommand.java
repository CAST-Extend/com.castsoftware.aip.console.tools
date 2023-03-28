package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.VersionDto;
import com.castsoftware.aip.console.tools.core.dto.VersionStatus;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobExecutionDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobRequestBuilder;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobType;
import com.castsoftware.aip.console.tools.core.dto.jobs.ScanAndReScanApplicationJobRequest;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApiKeyMissingException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.core.utils.SemVerUtils;
import com.castsoftware.aip.console.tools.providers.CliLogPollingProviderImpl;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Creates a snapshot for an application
 */
@Component
@CommandLine.Command(
        name = "Snapshot",
        mixinStandardHelpOptions = true,
        aliases = {"snapshot"},
        description = "Runs a snapshot on AIP Console"
)
@Slf4j
@Getter
@Setter
public class SnapshotCommand implements Callable<Integer> {
    private static final DateFormat RELEASE_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private final RestApiService restApiService;
    private final JobsService jobsService;
    private final ApplicationService applicationService;
    @CommandLine.Mixin
    private SharedOptions sharedOptions;

    @CommandLine.Option(names = {"-n", "--app-name"},
            paramLabel = "APPLICATION_NAME",
            description = "The Name of the application to analyze",
            required = true)
    private String applicationName;

    @CommandLine.Option(names = {"-v", "--version-name"},
            paramLabel = "VERSION_NAME", description = "The name of the version for which the snapshot will be run")
    private String versionName;

    @CommandLine.Option(names = {"-S", "--snapshot-name"},
            paramLabel = "SNAPSHOT_NAME",
            description = "The name of the snapshot to create")
    private String snapshotName;
    @CommandLine.Option(names = {"--snapshot-date"},
            description = "The snapshot date associated with the snapshot to be create: the expected format is \"yyyy-MM-ddTHH:mm:ss\"")
    private String snapshotDateString;

    @CommandLine.Option(names = "--process-imaging", description = "If provided, will upload data to Imaging"
            + " if specified without parameter: ${FALLBACK-VALUE}",
            fallbackValue = "true")
    private boolean processImaging = false;
    @CommandLine.Option(names = {"--consolidation", "--upload-application"},
            description = "When sets to false,  this prevents from consolidating snapshot or from publishing application to the Health dashboard"
                    + " if specified without parameter: ${FALLBACK-VALUE}",
            defaultValue = "true", fallbackValue = "true")
    private boolean consolidation = true;
    @CommandLine.Option(names = {"--sleep-duration"},
            description = "Number of seconds used to refresh the ongoing job status. The default value is: ${DEFAULT-VALUE}",
            defaultValue = "15")
    private long sleepDuration;

    public SnapshotCommand(RestApiService restApiService, JobsService jobsService, ApplicationService applicationService) {
        this.restApiService = restApiService;
        this.jobsService = jobsService;
        this.applicationService = applicationService;
    }


    @Override
    public Integer call() throws Exception {
        // Runs snapshot + upload
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
        ApiInfoDto apiInfoDto = restApiService.getAipConsoleApiInfo();

        try {
            log.info("Searching for application '{}' on AIP Console", applicationName);
            ApplicationDto app = applicationService.getApplicationFromName(applicationName);
            String applicationGuid;
            if (app == null || StringUtils.isBlank(app.getGuid())) {
                log.error("Application '{}' was not found on AIP Console", applicationName);
                return Constants.RETURN_APPLICATION_NOT_FOUND;
            }
            applicationGuid = app.getGuid();
            Set<VersionDto> versions = applicationService.getApplicationVersion(applicationGuid);
            if (versions.isEmpty()) {
                log.error("No version for the given application. Cannot run Snapshot without an analyzed version");
                return Constants.RETURN_APPLICATION_NO_VERSION;
            }
            if (versions.stream().noneMatch(v -> v.getStatus().ordinal() >= VersionStatus.ANALYSIS_DONE.ordinal())) {
                log.error("No analysis done for application '{}'. Cannot create snapshot.", applicationName);
                return Constants.RETURN_VERSION_WITH_ANALYSIS_DONE_NOT_FOUND;
            }
            VersionDto foundVersion;
            if (StringUtils.isNotBlank(versionName)) {
                Optional<VersionDto> optionalVersionDto = versions.stream()
                        .filter(v -> StringUtils.equalsIgnoreCase(v.getName(), versionName))
                        .findFirst();
                if (!optionalVersionDto.isPresent()) {
                    log.error("No version found with name " + versionName);
                    return Constants.RETURN_APPLICATION_VERSION_NOT_FOUND;
                }
                foundVersion = optionalVersionDto.get();
            } else {
                Optional<VersionDto> optionalVersionDto = versions
                        .stream()
                        .filter(v -> v.getStatus().ordinal() >= VersionStatus.ANALYSIS_DONE.ordinal())
                        .max(Comparator.comparing(VersionDto::getVersionDate));
                if (!optionalVersionDto.isPresent()) {
                    log.error("No analyzed version found to create a snapshot for. Make sure you have at least one version that has been analyzed");
                    return Constants.RETURN_APPLICATION_VERSION_NOT_FOUND;
                }
                foundVersion = optionalVersionDto.get();
            }

            if (StringUtils.isBlank(snapshotName)) {
                snapshotName = String.format("Snapshot-%s", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date()));
            }

            //TODO: refactor after release to get separated workflows
            if (app.isOnboarded()) {
                log.info("Triggering snapshot for an application using Fast-Scan workflow.");
                ScanAndReScanApplicationJobRequest.ScanAndReScanApplicationJobRequestBuilder requestBuilder = ScanAndReScanApplicationJobRequest.builder()
                        .appGuid(applicationGuid);
                String targetNode = app.getTargetNode();
                if (StringUtils.isNotEmpty(targetNode)) {
                    requestBuilder.targetNode(targetNode);
                }
                String caipVersion = apiInfoDto.getApiVersion();
                if (StringUtils.isNotEmpty(caipVersion)) {
                    requestBuilder.caipVersion(caipVersion);
                }
                if (StringUtils.isNotEmpty(snapshotName)) {
                    requestBuilder.snapshotName(snapshotName);
                }

                requestBuilder.processImaging(processImaging);
                requestBuilder.publishToEngineering(processImaging || consolidation);
                requestBuilder.uploadApplication(true);

                CliLogPollingProviderImpl cliLogPolling = new CliLogPollingProviderImpl(jobsService, getSharedOptions().isVerbose(), sleepDuration);
                String appGuid = applicationService.runDeepAnalysis(requestBuilder.build(), cliLogPolling);
                if (StringUtils.isEmpty(appGuid)) {
                    log.error("Snapshot operating wasn't performed successfully. Toggle verbose ON or check CAST Console logs for more details.");
                    return Constants.RETURN_JOB_FAILED;
                }
                return Constants.RETURN_OK;
            }

            // Run snapshot in legacy workflow
            boolean forcedConsolidation = processImaging || consolidation;
            JobRequestBuilder builder = JobRequestBuilder.newInstance(applicationGuid, null, JobType.ANALYZE, app.getCaipVersion())
                    .nodeName(app.getTargetNode())
                    .startStep(Constants.SNAPSHOT_STEP_NAME)
                    .versionGuid(foundVersion.getGuid())
                    .versionName(foundVersion.getName())
                    .snapshotName(snapshotName)
                    .uploadApplication(true)
                    .snapshotDate(applicationService.getVersionDate(snapshotDateString))
                    .processImaging(processImaging)
                    .uploadApplication(forcedConsolidation)
                    .endStep(SemVerUtils.isNewerThan115(apiInfoDto.getApiVersionSemVer()) ?
                            Constants.UPLOAD_APP_SNAPSHOT : Constants.SNAPSHOT_INDICATOR);

            //Snapshot required now see whether we upload application or not
            if (!forcedConsolidation) {
                log.info("The snapshot for application {} will be taken but will not be published.", applicationName);
                builder.endStep(Constants.SNAPSHOT_INDICATOR);
            }

            log.info("Running Snapshot Job on application '{}' with Version '{}' (guid: '{}')", applicationName, foundVersion.getName(), foundVersion.getGuid());
            log.info("Job request : " + builder.buildJobRequest().toString());

            String jobGuid = jobsService.startJob(builder);

            Thread shutdownHook = getShutdownHookForJobGuid(jobGuid);

            Runtime.getRuntime().addShutdownHook(shutdownHook);
            JobExecutionDto jobStatus = jobsService.pollAndWaitForJobFinished(jobGuid, Function.identity(), sharedOptions.isVerbose());
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            if (JobState.COMPLETED == jobStatus.getState()) {
                log.info("Snapshot Creation completed successfully.");
                return Constants.RETURN_OK;
            }
            log.error("Snapshot Job did not complete. Status is '{}' on step '{}'", jobStatus.getState(), jobStatus.getCurrentStep());
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
