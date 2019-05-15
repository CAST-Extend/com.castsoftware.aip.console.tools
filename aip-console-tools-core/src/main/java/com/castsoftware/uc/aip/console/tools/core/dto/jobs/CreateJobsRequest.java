package com.castsoftware.uc.aip.console.tools.core.dto.jobs;

import java.util.HashMap;
import java.util.Map;

public class CreateJobsRequest {
    private JobType jobType;
    private Map<String, String> jobParameters = new HashMap<>();

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public Map<String, String> getJobParameters() {
        return jobParameters;
    }

    public String getParameterValueAsString(String key) {
        if (jobParameters.containsKey(key)) {
            return jobParameters.get(key);
        }
        return null;
    }

    public void setJobParameters(Map<String, String> jobParameters) {
        this.jobParameters = jobParameters;
    }
}
