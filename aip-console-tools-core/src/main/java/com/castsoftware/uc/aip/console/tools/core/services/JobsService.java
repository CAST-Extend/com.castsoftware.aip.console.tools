package com.castsoftware.uc.aip.console.tools.core.services;

import com.castsoftware.uc.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.uc.aip.console.tools.core.dto.jobs.JobStatusWithSteps;
import com.castsoftware.uc.aip.console.tools.core.exceptions.JobServiceException;

import java.util.Date;
import java.util.function.Consumer;
import java.util.function.Function;

public interface JobsService {
    String startCreateApplication(String applicationName) throws JobServiceException;

    String startAddVersionJob(String appGuid, String zipFileName, String versionName, Date versionReleaseDate, boolean cloneVersion) throws JobServiceException;

    JobState pollAndWaitForJobFinished(String jobGuid) throws JobServiceException;

    <R> R pollAndWaitForJobFinished(String jobGuid, Function<JobStatusWithSteps, R> completionCallback) throws JobServiceException;

    <R> R pollAndWaitForJobFinished(String jobGuid, Consumer<JobStatusWithSteps> stepChangedCallback, Function<JobStatusWithSteps, R> completionCallback) throws JobServiceException;
}
