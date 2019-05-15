package com.castsoftware.uc.aip.console.tools.core.services;

import com.castsoftware.uc.aip.console.tools.core.dto.VersionStatus;
import com.castsoftware.uc.aip.console.tools.core.dto.Versions;
import com.castsoftware.uc.aip.console.tools.core.dto.jobs.ChangeJobStateRequest;
import com.castsoftware.uc.aip.console.tools.core.dto.jobs.CreateJobsRequest;
import com.castsoftware.uc.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.uc.aip.console.tools.core.dto.jobs.JobStatus;
import com.castsoftware.uc.aip.console.tools.core.dto.jobs.JobStatusWithSteps;
import com.castsoftware.uc.aip.console.tools.core.dto.jobs.JobType;
import com.castsoftware.uc.aip.console.tools.core.dto.jobs.SuccessfulJobStartDto;
import com.castsoftware.uc.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.uc.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.uc.aip.console.tools.core.utils.ApiEndpointHelper;
import com.castsoftware.uc.aip.console.tools.core.utils.Constants;
import lombok.extern.java.Log;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

@Log
public class JobsServiceImpl implements JobsService {

    private static final DateFormat formatReleaseDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
    private static final long POLL_SLEEP_DURATION = TimeUnit.SECONDS.toMillis(10);

    private RestApiService restApiService;

    public JobsServiceImpl(RestApiService restApiService) {
        this.restApiService = restApiService;
    }

    @Override
    public String startCreateApplication(String applicationName) throws JobServiceException {
        assert StringUtils.isNotBlank(applicationName);
        Map<String, String> jobParams = new HashMap<>();
        jobParams.put(Constants.PARAM_APP_NAME, applicationName);
        //FIXME add target node ? Get by name ? GUID ? Or let AIP Console handle it ?
        //jobParams.put(JobConstants.PARAM_NODE_GUID, nodeGuid);

        try {
            String jobsEndpoint = ApiEndpointHelper.getJobsEndpoint();
            CreateJobsRequest request = new CreateJobsRequest();
            request.setJobType(JobType.DECLARE_APPLICATION);
            request.setJobParameters(jobParams);
            SuccessfulJobStartDto jobStartDto = restApiService.postForEntity(jobsEndpoint, request, SuccessfulJobStartDto.class);
            return jobStartDto.getJobGuid();
        } catch (ApiCallException e) {
            log.log(Level.SEVERE, "Unable to create new application '" + applicationName + "'", e);
            throw new JobServiceException("Creation of application failed", e);
        }
    }

    @Override
    public String startAddVersionJob(String appGuid, String zipFileName, String versionName, Date versionReleaseDate, boolean cloneVersion)
            throws JobServiceException {
        assert StringUtils.isNoneEmpty(appGuid, zipFileName, versionName);

        String jobsEndpoint = ApiEndpointHelper.getJobsEndpoint();

        if (cloneVersion) {
            try {
                String versionsEndpoint = ApiEndpointHelper.getApplicationVersionsPath(appGuid);
                Versions versions = restApiService.getForEntity(versionsEndpoint, Versions.class);
                if (versions == null ||
                        versions.getVersions() == null ||
                        versions.getVersions().stream().noneMatch(v -> v.getStatus().ordinal() > VersionStatus.OPENED.ordinal())) {
                    log.log(Level.SEVERE, "Cannot clone version, there is no version to copy.");
                    throw new JobServiceException("Cannot clone version, there is no version to copy.");
                }
            } catch (ApiCallException e) {
                log.warning("Unable to check versions for application. Starting job may fail.");
            }
        }

        Map<String, String> jobParameters = new HashMap<>();
        jobParameters.put(Constants.PARAM_APP_GUID, appGuid);
        String fileName = FilenameUtils.getName(zipFileName);
        jobParameters.put(Constants.PARAM_SOURCE_ARCHIVE, FilenameUtils.getName(zipFileName));
        jobParameters.put("fileName", FilenameUtils.getName(zipFileName));
        jobParameters.put(Constants.PARAM_VERSION_NAME, versionName);
        jobParameters.put(Constants.PARAM_START_STEP, Constants.EXTRACT_STEP_NAME);
        jobParameters.put(Constants.PARAM_END_STEP, Constants.CONSOLIDATE_STEP_NAME);
        if (versionReleaseDate != null) {
            String versionReleaseStr = formatReleaseDate.format(versionReleaseDate);
            log.info(String.format("Creating version '%s' for application '%s' with release date '%s'", versionName, appGuid, versionReleaseStr));

            jobParameters.put(Constants.PARAM_RELEASE_DATE, versionReleaseStr);
            jobParameters.put(Constants.PARAM_SNAPSHOT_CAPTURE_DATE, versionReleaseStr);
        } else {
            log.info(String.format("Creating version '%s' for application '%s'", versionName, appGuid));
        }
        CreateJobsRequest jobsRequest = new CreateJobsRequest();

        if (cloneVersion) {
            jobsRequest.setJobType(JobType.CLONE_VERSION);
        } else {
            jobsRequest.setJobType(JobType.ADD_VERSION);
        }
        jobsRequest.setJobParameters(jobParameters);

        try {
            SuccessfulJobStartDto dto = restApiService.postForEntity(jobsEndpoint, jobsRequest, SuccessfulJobStartDto.class);
            assert dto != null;
            assert StringUtils.isNotBlank(dto.getJobGuid());

            // Add_Version may be started in "suspended" mode.
            // We check the status and start it automatically if it is not started yet
            String jobDetailsEndpoint = ApiEndpointHelper.getJobDetailsEndpoint(dto.getJobGuid());
            JobStatusWithSteps jobStatusWithSteps = restApiService.getForEntity(jobDetailsEndpoint, JobStatusWithSteps.class);
            if (jobStatusWithSteps.getState() == JobState.STARTING) {
                log.finest("Resuming suspended job");
                ChangeJobStateRequest resumeRequest = new ChangeJobStateRequest();
                resumeRequest.setState(JobState.STARTED);
                restApiService.putForEntity(jobDetailsEndpoint, resumeRequest, String.class);
            }
            log.info("Successfully started Job");
            return dto.getJobGuid();
        } catch (ApiCallException e) {
            log.log(Level.SEVERE, "Error starting add version job.", e);
            throw new JobServiceException(e);
        }
    }

    @Override
    public JobState pollAndWaitForJobFinished(String jobGuid) throws JobServiceException {
        return pollAndWaitForJobFinished(jobGuid, JobStatus::getState);
    }

    @Override
    public <R> R pollAndWaitForJobFinished(String jobGuid, Function<JobStatusWithSteps, R> callback) throws JobServiceException {
        return pollAndWaitForJobFinished(jobGuid,
                jobStep -> log.info("Current step is : " +  jobStep.getProgressStep()),
                callback);
    }

    @Override
    public <R> R pollAndWaitForJobFinished(String jobGuid, Consumer<JobStatusWithSteps> stepChangedCallback, Function<JobStatusWithSteps, R> completionCallback) throws JobServiceException {
        assert StringUtils.isNotBlank(jobGuid);

        String jobDetailsEndpoint = ApiEndpointHelper.getJobDetailsEndpoint(jobGuid);
        String previousStep = "";
        log.fine("Checking status of Job with GUID " + jobGuid);
        try {
            JobStatusWithSteps jobStatus;
            while (true) {
                jobStatus = restApiService.getForEntity(jobDetailsEndpoint, JobStatusWithSteps.class);
                String currentStep = jobStatus.getProgressStep();

                if (currentStep != null && !currentStep.equalsIgnoreCase(previousStep)) {
                    previousStep = currentStep;
                    if(stepChangedCallback != null) {
                        stepChangedCallback.accept(jobStatus);
                    }
                }

                if (jobStatus.getState() != JobState.STARTED && jobStatus.getState() != JobState.STARTING) {
                    break;
                }

                Thread.sleep(POLL_SLEEP_DURATION);
            }
            return completionCallback.apply(jobStatus);
        } catch (ApiCallException | InterruptedException e) {
            log.log(Level.SEVERE, "Error occurred while polling the job status", e);
            throw new JobServiceException(e);
        }
    }
}
