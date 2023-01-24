package com.castsoftware.aip.console.tools.core.services;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.DatabaseConnectionSettingsDto;
import com.castsoftware.aip.console.tools.core.dto.DeliveryConfigurationDto;
import com.castsoftware.aip.console.tools.core.dto.ModuleGenerationType;
import com.castsoftware.aip.console.tools.core.dto.jobs.ChangeJobStateRequest;
import com.castsoftware.aip.console.tools.core.dto.jobs.CreateApplicationJobRequest;
import com.castsoftware.aip.console.tools.core.dto.jobs.CreateJobsRequest;
import com.castsoftware.aip.console.tools.core.dto.jobs.DiscoverApplicationJobRequest;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobExecutionDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobRequestBuilder;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobType;
import com.castsoftware.aip.console.tools.core.dto.jobs.LogContentDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.LogsDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.OnboardApplicationJobRequest;
import com.castsoftware.aip.console.tools.core.dto.jobs.PublishApplicationJobRequest;
import com.castsoftware.aip.console.tools.core.dto.jobs.ScanAndReScanApplicationJobRequest;
import com.castsoftware.aip.console.tools.core.dto.jobs.SuccessfulJobStartDto;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.utils.ApiEndpointHelper;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.core.utils.LogUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

import static com.castsoftware.aip.console.tools.core.utils.Constants.PARAM_CAIP_VERSION;
import static com.castsoftware.aip.console.tools.core.utils.Constants.PARAM_TARGET_NODE;

@Log
public class JobsServiceImpl implements JobsService {
    private static final long POLL_SLEEP_DURATION = TimeUnit.SECONDS.toMillis(15);

    private final RestApiService restApiService;

    private final long pollingSleepDuration;

    private ApiInfoDto apiInfoDto;

    public JobsServiceImpl(RestApiService restApiService) {
        this.restApiService = restApiService;
        this.pollingSleepDuration = POLL_SLEEP_DURATION;
    }

    public JobsServiceImpl(RestApiService restApiService, long pollingSleepDuration) {
        this.restApiService = restApiService;
        this.pollingSleepDuration = pollingSleepDuration;
    }

    @Override
    public String startCreateApplication(String applicationName, boolean inplaceMode, String caipVersion) throws JobServiceException {
        if (StringUtils.isBlank(applicationName)) {
            throw new JobServiceException("Application name is empty. Unable to create application");
        }
        return startCreateApplication(applicationName, null, inplaceMode, caipVersion);
    }

    @Override
    public String startCreateApplication(String applicationName, String nodeName, boolean inplaceMode, String caipVersion) throws JobServiceException {
        return startCreateApplication(applicationName, nodeName, null, inplaceMode, caipVersion, null);
    }

    @Override
    public String startOnboardApplication(String applicationName, String nodeName, String domainName, String caipVersion) throws JobServiceException {
        log.log(Level.INFO, "Onboarding Application on NODE: " + (StringUtils.isNotEmpty(nodeName) ? nodeName : "Default (auto-selected)"));
        OnboardApplicationJobRequest.OnboardApplicationJobRequestBuilder requestBuilder = OnboardApplicationJobRequest.builder()
                .appName(applicationName);
        if (StringUtils.isNotEmpty(caipVersion)) {
            requestBuilder.caipVersion(caipVersion);
        }
        if (StringUtils.isNotEmpty(domainName)) {
            requestBuilder.domainName(domainName);
        }

        try {
            SuccessfulJobStartDto jobStartDto = restApiService.postForEntity(ApiEndpointHelper.getOnboardApplicationEndPoint(), requestBuilder.build(), SuccessfulJobStartDto.class);
            return jobStartDto.getJobGuid();
        } catch (ApiCallException e) {
            log.log(Level.SEVERE, "Unable to create new application '" + applicationName + "'", e);
            throw new JobServiceException("Creation of application failed", e);
        }
    }

    @Override
    public String startDiscoverApplication(String applicationGuid, String sourcePath, String versionName, String caipVersion, String targetNode) throws JobServiceException {
        DiscoverApplicationJobRequest.DiscoverApplicationJobRequestBuilder requestBuilder = DiscoverApplicationJobRequest.builder()
                .appGuid(applicationGuid);

        if (StringUtils.isNotEmpty(sourcePath)) {
            requestBuilder.sourcePath(caipVersion);
        }

        if (StringUtils.isNotEmpty(caipVersion)) {
            requestBuilder.caipVersion(caipVersion);
        }
        if (StringUtils.isNotEmpty(targetNode)) {
            requestBuilder.targetNode(targetNode);
        }
        if (StringUtils.isNotEmpty(versionName)) {
            requestBuilder.versionName(versionName);
        }
        try {
            SuccessfulJobStartDto jobStartDto = restApiService.postForEntity(ApiEndpointHelper.getDiscoverApplicationEndPoint(), requestBuilder.build(), SuccessfulJobStartDto.class);
            return jobStartDto.getJobGuid();
        } catch (ApiCallException e) {
            log.log(Level.SEVERE, "Unable to discover application '" + applicationGuid + "' providing source path: " + sourcePath, e);
            throw new JobServiceException("Creation of application failed", e);
        }
    }

    @Override
    public String startReDiscoverApplication(String applicationGuid, String sourcePath, String versionName, DeliveryConfigurationDto deliveryConfig, String caipVersion, String targetNode) throws JobServiceException {
        DiscoverApplicationJobRequest.DiscoverApplicationJobRequestBuilder requestBuilder = DiscoverApplicationJobRequest.builder()
                .appGuid(applicationGuid)
                .sourcePath(sourcePath)
                .deliveryConfigGuid(deliveryConfig.getGuid())
                .ignorePatterns(deliveryConfig.getIgnorePatterns())
                .exclusionRules(deliveryConfig.getExclusionRules());

        if (StringUtils.isNotEmpty(caipVersion)) {
            requestBuilder.caipVersion(caipVersion);
        }
        if (StringUtils.isNotEmpty(targetNode)) {
            requestBuilder.targetNode(targetNode);
        }
        if (StringUtils.isNotEmpty(versionName)) {
            requestBuilder.versionName(versionName);
        }
        try {
            SuccessfulJobStartDto jobStartDto = restApiService.postForEntity(ApiEndpointHelper.getReDiscoverApplicationEndPoint(), requestBuilder.build(), SuccessfulJobStartDto.class);
            return jobStartDto.getJobGuid();
        } catch (ApiCallException e) {
            log.log(Level.SEVERE, "Unable to ReDiscover application '" + applicationGuid + "' providing sources path: " + sourcePath, e);
            throw new JobServiceException("ReDiscover application failed", e);
        }
    }

    @Override
    public String startRunFirstScanApplication(String applicationGuid, String nodeName, String caipVersion) throws JobServiceException {
        ScanAndReScanApplicationJobRequest.ScanAndReScanApplicationJobRequestBuilder requestBuilder = ScanAndReScanApplicationJobRequest.builder();
        requestBuilder.appGuid(applicationGuid);
        if (StringUtils.isNotEmpty(nodeName)) {
            requestBuilder.targetNode(nodeName);
        }
        if (StringUtils.isNotEmpty(caipVersion)) {
            requestBuilder.caipVersion(caipVersion);
        }

        try {
            SuccessfulJobStartDto jobStartDto = restApiService.postForEntity(ApiEndpointHelper.getFirstScanEndPoint(), requestBuilder.build(), SuccessfulJobStartDto.class);
            return jobStartDto.getJobGuid();
        } catch (ApiCallException e) {
            log.log(Level.SEVERE, "Unable to perform application First-Scan action (Run Analysis)", e);
            throw new JobServiceException("The First-Scan of application failed", e);
        }
    }

    @Override
    public String startRunReScanApplication(String applicationGuid, String nodeName, String caipVersion) throws JobServiceException {
        ScanAndReScanApplicationJobRequest.ScanAndReScanApplicationJobRequestBuilder requestBuilder = ScanAndReScanApplicationJobRequest.builder()
                .appGuid(applicationGuid);
        if (StringUtils.isNotEmpty(nodeName)) {
            requestBuilder.targetNode(nodeName);
        }
        if (StringUtils.isNotEmpty(caipVersion)) {
            requestBuilder.caipVersion(caipVersion);
        }

        try {
            SuccessfulJobStartDto jobStartDto = restApiService.postForEntity(ApiEndpointHelper.getReScanApplicationEndPoint(), requestBuilder.build(), SuccessfulJobStartDto.class);
            return jobStartDto.getJobGuid();
        } catch (ApiCallException e) {
            log.log(Level.SEVERE, "Unable to perform ReScan application action (Run Analysis)", e);
            throw new JobServiceException("ReScan application job failed", e);
        }
    }

    @Override
    public String startPublishToImaging(String applicationGuid, String nodeName, String caipVersion) throws JobServiceException {
        PublishApplicationJobRequest request = PublishApplicationJobRequest.builder().appGuid(applicationGuid).build();
        try {
            SuccessfulJobStartDto jobStartDto = restApiService.postForEntity(ApiEndpointHelper.getPublishToImagingEndPoint(), request, SuccessfulJobStartDto.class);
            return jobStartDto.getJobGuid();
        } catch (ApiCallException e) {
            log.log(Level.SEVERE, "Unable to perform ReScan application action (Run Analysis)", e);
            throw new JobServiceException("ReScan application job failed", e);
        }
    }

    @Override
    public String startCreateApplication(String applicationName, String nodeName, String domainName, boolean inplaceMode, String caipVersion, String cssServerName) throws JobServiceException {
        String cssServerGuid = getCssGuid(cssServerName);
        if (cssServerGuid != null) {
            log.log(Level.INFO,
                    "Application " + applicationName + " data repository will stored in CSS Server " + cssServerName + "(guid: " + cssServerGuid + ")");
        } else {
            log.log(Level.INFO,
                    "Application " + applicationName + " data repository will stored on default CSS server");
        }

        log.log(Level.INFO, "Starting job: Create Application on NODE: " + (StringUtils.isNotEmpty(nodeName) ? nodeName : "Default (auto-selected)"));
        try {
            CreateApplicationJobRequest.CreateApplicationJobRequestBuilder requestBuilder =
                    CreateApplicationJobRequest.builder()
                            .appName(applicationName)
                            .inPlaceMode(inplaceMode)
                            .caipVersion(caipVersion)
                            .cssGuid(cssServerGuid);
            if(StringUtils.isNotBlank(nodeName)) {
                requestBuilder.targetNode(nodeName);
            }
            if (StringUtils.isNotBlank(domainName)) {
                requestBuilder.domainName(domainName);
            }
            SuccessfulJobStartDto jobStartDto = restApiService.postForEntity(ApiEndpointHelper.getCreateApplicationEndPoint(), requestBuilder.build(), SuccessfulJobStartDto.class);
            return jobStartDto.getJobGuid();
        } catch (ApiCallException e) {
            log.log(Level.SEVERE, "Unable to create new application '" + applicationName + "'", e);
            throw new JobServiceException("Creation of application failed", e);
        }
    }

    @Override
    public String getCssGuid(String cssServerName) throws JobServiceException {
        if (StringUtils.isNotEmpty(cssServerName)) {
            try {
                DatabaseConnectionSettingsDto[] cssServers = restApiService.getForEntity("api/settings/css-settings",
                        DatabaseConnectionSettingsDto[].class);
                Optional<DatabaseConnectionSettingsDto> targetCss = Arrays.stream(cssServers).filter(db -> buildCssServerName(db).equalsIgnoreCase(cssServerName)).findFirst();
                if (targetCss.isPresent()) {
                    return targetCss.get().getGuid();
                } else {
                    log.log(Level.SEVERE, "Target CSS database with following name does not exist or check the format: " + cssServerName);
                    throw new JobServiceException("Target CSS database with following name does not exist: " + cssServerName);
                }
            } catch (ApiCallException e) {
                log.log(Level.SEVERE, "Call to CAST Console resulted in an error.", e);
                throw new JobServiceException("Target CSS database with following name does not exist: " + cssServerName, e);
            }
        }
        return null;
    }

    private String buildCssServerName(DatabaseConnectionSettingsDto db) {
        return db.getServerName() + "/" + db.getDatabaseName();
    }

    @Override
    public String startAddVersionJob(String appGuid, String applicationName, String caipVersion, String zipFileName, String versionName, Date versionReleaseDate, boolean cloneVersion)
            throws JobServiceException {
        return startAddVersionJob(appGuid, applicationName, caipVersion, zipFileName, versionName, versionReleaseDate, cloneVersion, false);
    }

    @Override
    public String startAddVersionJob(String appGuid, String applicationName, String caipVersion, String sourcePath, String versionName, Date versionReleaseDate, boolean cloneVersion, boolean enableSecurityDataflow)
            throws JobServiceException {
        if (StringUtils.isBlank(appGuid)) {
            throw new JobServiceException("No application GUID provided");
        }
        if (StringUtils.isBlank(sourcePath)) {
            throw new JobServiceException("No Archive File name provided to create the new version");
        }
        if (versionReleaseDate == null) {
            throw new JobServiceException("No release date provided.");
        }
        if (StringUtils.isBlank(versionName)) {
            DateFormat formatVersionName = new SimpleDateFormat("yyMMdd.HHmmss");
            versionName = "v" + formatVersionName.format(versionReleaseDate);
        }
        JobRequestBuilder builder = JobRequestBuilder.newInstance(appGuid, sourcePath, cloneVersion ? JobType.CLONE_VERSION : JobType.ADD_VERSION, caipVersion)
                .versionName(versionName)
                .releaseAndSnapshotDate(versionReleaseDate)
                .securityObjective(enableSecurityDataflow);

        return startAddVersionJob(builder);
    }

    @Override
    public String startAddVersionJob(JobRequestBuilder builder) throws JobServiceException {
        builder.startStep(Constants.EXTRACT_STEP_NAME);
        return startJob(builder);
    }

    @Override
    public String startJob(JobRequestBuilder jobRequestBuilder) throws JobServiceException {
        CreateJobsRequest jobRequest = filterModuleGenerationType(jobRequestBuilder.buildJobRequest());
        ApiInfoDto apiInfoDto = getApiInfoDto();

        String caipVersion = jobRequest.getParameterValueAsString(PARAM_CAIP_VERSION);
        String targetNode = jobRequest.getParameterValueAsString(PARAM_TARGET_NODE);
        String nodeMessage = (StringUtils.isEmpty(targetNode) ? "Default (auto-selected)" : targetNode);
        String message = "NODE: " + nodeMessage + " and AIP version: " + (StringUtils.isEmpty(caipVersion) ? "node default configured" : caipVersion);
        log.info("Starting job on " + message);

        try {
            SuccessfulJobStartDto dto = restApiService.postForEntity(ApiEndpointHelper.getJobsEndpoint(jobRequest), jobRequest, SuccessfulJobStartDto.class);

            if (dto == null || StringUtils.isBlank(dto.getJobGuid())) {
                throw new JobServiceException("No response from AIP Console when start the job");
            }

            // We are in V2 not need to resume job anymore
            log.info("Successfully started Job");
            return dto.getJobGuid();
        } catch (ApiCallException e) {
            log.log(Level.SEVERE, "Error starting Job with type " + jobRequest.getJobType(), e);
            throw new JobServiceException(e);
        }
    }

    @Override
    public JobState pollAndWaitForJobFinished(String jobGuid) throws JobServiceException {
        return pollAndWaitForJobFinished(jobGuid, JobExecutionDto::getState, true);
    }

    @Override
    public <R> R pollAndWaitForJobFinished(String jobGuid, Function<JobExecutionDto, R> callback, boolean logOutput) throws JobServiceException {
        Consumer<LogContentDto> pollingCallback = !logOutput ? null : jobContentDto -> printLog(jobContentDto);
        return pollAndWaitForJobFinished(jobGuid,
                jobStep -> log.info("Current step is : " + jobStep.getCurrentStep()),
                pollingCallback,
                callback);
    }

    @Override
    public <R> R pollAndWaitForJobFinished(String jobGuid, Consumer<JobExecutionDto> stepChangedCallback, Consumer<LogContentDto> pollingCallback, Function<JobExecutionDto, R> completionCallback) throws JobServiceException {
        assert StringUtils.isNotBlank(jobGuid);

        String jobDetailsEndpoint = ApiEndpointHelper.getJobDetailsEndpoint(jobGuid);
        String previousStep = "";
        log.fine("Checking status of Job with GUID " + jobGuid);
        int retryCount = 0;
        try {
            JobExecutionDto jobStatus;
            String logName = null;
            int startOffset = 0;
            while (true) {
                Thread.sleep(pollingSleepDuration);
                // Force login to keep session alive (jobs endpoint doesn't refresh session status)
                restApiService.login();

                // Sometimes it takes more than 10 secs till the jobstatus is ready
                jobStatus = getJobStatus(jobDetailsEndpoint);
                if (jobStatus == null) {
                    if (retryCount < 20) {
                        ++retryCount;
                        continue;
                    } else {
                        throw new ApiCallException(500, "Failed to get job status after retrying 20 times");
                    }

                } else {
                    retryCount = 0;
                }

                String currentStep = jobStatus.getCurrentStep();

                if (currentStep != null && !currentStep.equalsIgnoreCase(previousStep)) {
                    previousStep = currentStep;
                    if (stepChangedCallback != null) {
                        stepChangedCallback.accept(jobStatus);
                    }
                    logName = getLogName(jobGuid, currentStep);
                    startOffset = 0;
                }

                if (pollingCallback != null && !StringUtils.isAnyBlank(logName, currentStep)) {
                    LogContentDto logContent = getLogContent(jobGuid, currentStep, logName, startOffset);
                    if (logContent != null && !logContent.getLines().isEmpty()) {
                        pollingCallback.accept(logContent);
                        startOffset = startOffset + logContent.getNbLines();
                    }
                }

                if (jobStatus.getState() != JobState.STARTED && jobStatus.getState() != JobState.STARTING) {
                    break;
                }
            }
            return completionCallback.apply(jobStatus);
        } catch (InterruptedException | ApiCallException e) {
            log.log(Level.SEVERE, "Error occurred while polling the job status", e);
            throw new JobServiceException(e);
        }
    }

    @Override
    public void cancelJob(String jobGuid) throws JobServiceException {
        try {
            ChangeJobStateRequest cancel = new ChangeJobStateRequest();
            cancel.setState(JobState.CANCELED);
            restApiService.postForEntity(ApiEndpointHelper.getJobDetailsEndpoint(jobGuid) + "/cancel", cancel, Void.class);
        } catch (ApiCallException e) {
            throw new JobServiceException(e);
        }
    }

    private LogContentDto getLogContent(String jobGuid, String currentStep, String logName, int startOffset) {
        try {
            return restApiService.getForEntity("/api/jobs/" + jobGuid + "/steps/" + currentStep + "/logs/" + logName + "?nbLines=3000&startOffset=" + startOffset, LogContentDto.class);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error to get the log content", e);
            return null;
        }
    }

    private String getLogName(String jobGuid, String step) throws ApiCallException {
        try {
            Set<LogsDto> logs = restApiService.getForEntity("/api/jobs/" + jobGuid + "/steps/" + step + "/logs", new TypeReference<Set<LogsDto>>() {
            });
            return logs.stream().filter(l -> l.getLogType().equalsIgnoreCase("MAIN_LOG")).findFirst().map(LogsDto::getLogName).orElse(null);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error to get the log name");
            return null;
        }
    }

    private JobExecutionDto getJobStatus(String jobDetailsEndpoint) {
        try {
            return restApiService.getForEntity(jobDetailsEndpoint, JobExecutionDto.class);
        } catch (ApiCallException e) {
            log.log(Level.SEVERE, "Error to get job status " + jobDetailsEndpoint);
            return null;
        }
    }

    private void printLog(LogContentDto logContent) {
        logContent.getLines().forEach(logLine -> log.info(LogUtils.replaceAllSensitiveInformation(logLine.getContent())));
    }

    private synchronized ApiInfoDto getApiInfoDto() {
        if (apiInfoDto == null) {
            apiInfoDto = restApiService.getAipConsoleApiInfo();
        }
        return apiInfoDto;
    }

    private CreateJobsRequest filterModuleGenerationType(CreateJobsRequest jobRequest) {
        String moduleTypeKey = "moduleGenerationType";
        String moduleTypeValue = (String) jobRequest.getJobParameters().get(moduleTypeKey);
        ModuleGenerationType moduleType = ModuleGenerationType.fromString(moduleTypeValue);
        if (moduleType != null && EnumSet.of(JobType.ANALYZE, JobType.CLONE_VERSION).contains(jobRequest.getJobType()) && moduleType == ModuleGenerationType.ONE_PER_TECHNO) {
            log.warning("Only following Module generation type are allowed: " + ModuleGenerationType.getAllowed(moduleType));
            jobRequest.getJobParameters().remove(moduleTypeKey);
        } else if (moduleType != null) {
            log.info("Applying Module generation type of " + moduleType);
        }
        return jobRequest;
    }
}