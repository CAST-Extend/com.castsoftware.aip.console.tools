package com.castsoftware.aip.console.tools.core.dto.jobs;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class CreateJobsRequest {
    private JobType jobType;
    private Map<String, Object> jobParameters = new HashMap<>();

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public Map<String, Object> getJobParameters() {
        return jobParameters;
    }

    public String getParameterValueAsString(String key) {
        if (jobParameters.containsKey(key)) {
            return String.valueOf(jobParameters.get(key));
        }
        return null;
    }

    public void setJobParameters(Map<String, Object> jobParameters) {
        this.jobParameters = jobParameters;
    }
}
