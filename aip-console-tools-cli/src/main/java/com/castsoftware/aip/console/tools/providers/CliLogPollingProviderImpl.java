package com.castsoftware.aip.console.tools.providers;

import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.LogPollingProvider;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.services.JobsService;

public class CliLogPollingProviderImpl implements LogPollingProvider {
    private final boolean verbose;
    private final JobsService jobsService;

    public CliLogPollingProviderImpl(JobsService jobsService, boolean verbose) {
        this.verbose = verbose;
        this.jobsService = jobsService;
    }

    @Override
    public String pollJobLog(String jobGuid) throws JobServiceException {
        return jobsService.pollAndWaitForJobFinished(jobGuid,
                (s) -> s.getState() == JobState.COMPLETED ? s.getJobParameters().get("appGuid") : null,
                verbose);
    }
}