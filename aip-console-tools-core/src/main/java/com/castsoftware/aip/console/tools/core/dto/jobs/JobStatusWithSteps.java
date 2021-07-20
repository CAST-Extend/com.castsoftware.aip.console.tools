package com.castsoftware.aip.console.tools.core.dto.jobs;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JobStatusWithSteps extends JobStatus {
    private String failureStep;
    private String currentStep;

    public JobStatusWithSteps() {
        // Default constructor
    }

    public JobStatusWithSteps(JobStatus status, String currentStep, String failureStep) {
        super(status);
        this.currentStep = currentStep;
        this.failureStep = failureStep;
    }

    public String getFailureStep() {
        return failureStep;
    }

    public void setFailureStep(String failureStep) {
        this.failureStep = failureStep;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }
}
