package com.castsoftware.aip.console.tools.core.dto.jobs;


import com.castsoftware.aip.console.tools.core.dto.serializers.StatusStringMapSerializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class JobStatus {

    private JobState state;

    private String jobType;

    private Map<String, String> jobParameters;

    private String guid;

    private String url;

    private String appGuid;

    private String appName;

    private List<String> fullSteps;

    private Map<String, String> logUrls;

    private Map<String, String> logfiles;

    private Date created;

    private Date updated;

    public JobStatus() {
    }

    public JobStatus(String guid, String url, JobType type, JobState state, Map<String, String> parameters,
                     Date created, Date updated, List<String> fullSteps, Map<String, String> logUrls, Map<String, String> logfiles) {
        this.guid = guid;
        this.url = url;
        this.jobType = type.getSerializableValue();
        this.state = state;
        this.jobParameters = parameters;
        this.created = created;
        this.updated = updated;
        this.fullSteps = fullSteps;
        this.logUrls = logUrls;
        this.logfiles = logfiles;
    }

    public JobStatus(JobStatus status) {
        appGuid = status.getAppGuid();
        appName = status.getAppName();
        guid = status.getGuid();
        url = status.getUrl();
        jobType = status.getJobType();
        state = status.getState();
        jobParameters = status.getJobParameters();
        created = status.getCreated();
        updated = status.getUpdated();
        fullSteps = status.getFullSteps();
        logUrls = status.getLogUrls();
        logfiles = status.getLogfiles();
    }

    public JobState getState() {
        return state;
    }

    public void setState(JobState state) {
        this.state = state;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonSerialize(using = StatusStringMapSerializer.class)
    public Map<String, String> getLogUrls() {
        return logUrls;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, String> getLogfiles() {
        return logfiles;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getJobType() {
        return jobType;
    }

    public final Map<String, String> getJobParameters() {
        return jobParameters;
    }

    public String getGuid() {
        return guid;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<String> getFullSteps() {
        return fullSteps;
    }

    public String getAppGuid() {
        return appGuid;
    }

    public void setAppGuid(String appGuid) {
        this.appGuid = appGuid;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public String getUrl() {
        return url;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }
}
