package com.castsoftware.aip.console.tools.core.dto.jobs;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JobStatusWithSteps extends JobStatus {
    private String failureStep;
    private String progressStep;

    public JobStatusWithSteps() {
        // Default constructor
    }

    public JobStatusWithSteps(JobStatus status, String progressStep, String failureStep) {
        super(status);
        this.progressStep = progressStep;
        this.failureStep = failureStep;
    }

    public String getFailureStep() {
        return failureStep;
    }

    public void setFailureStep(String failureStep) {
        this.failureStep = failureStep;
    }

    public String getProgressStep() {
        return progressStep;
    }

    public void setProgressStep(String progressStep) {
        this.progressStep = progressStep;
    }
}
