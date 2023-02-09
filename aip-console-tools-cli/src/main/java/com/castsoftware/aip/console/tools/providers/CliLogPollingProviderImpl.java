package com.castsoftware.aip.console.tools.providers;

import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.LogContentDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.LogPollingProvider;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.utils.LogUtils;
import lombok.extern.java.Log;

import java.util.concurrent.TimeUnit;

@Log
public class CliLogPollingProviderImpl implements LogPollingProvider {
    private final boolean verbose;
    private final JobsService jobsService;
    private final long sleepDuration;

    public CliLogPollingProviderImpl(JobsService jobsService, boolean verbose, long sleepDuration) {
        this.verbose = verbose;
        this.jobsService = jobsService;
        this.sleepDuration = sleepDuration;
    }

    @Override
    public String pollJobLog(String jobGuid) throws JobServiceException {
        return jobsService.pollAndWaitForJobFinished(jobGuid,
                jobStep -> log.info("Current step is : " + jobStep.getCurrentStep()),
                !verbose ? null : this::printLog,
                (s) -> s.getState() == JobState.COMPLETED ? s.getJobParameters().get("appGuid") : null,
                () -> TimeUnit.SECONDS.toMillis(sleepDuration));
    }

    private void printLog(LogContentDto logContent) {
        logContent.getLines().forEach(logLine -> log.info(LogUtils.replaceAllSensitiveInformation(logLine.getContent())));
    }

}