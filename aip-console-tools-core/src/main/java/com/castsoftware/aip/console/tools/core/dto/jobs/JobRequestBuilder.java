package com.castsoftware.aip.console.tools.core.dto.jobs;

import com.castsoftware.aip.console.tools.core.utils.Constants;
import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobRequestBuilder {
    private static final DateFormat RELEASE_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final DateFormat VERSION_NAME_FORMATTER = new SimpleDateFormat("yyMMdd.HHmmss");
    private static final String GLOBAL_RISK_OBJECTIVE = "GLOBAL_RISK";
    private static final String FUNCTIONAL_POINTS_OBJECTIVE = "FUNCTIONAL_POINTS";
    private static final String SECURITY_OBJECTIVE = "SECURITY";

    private String appGuid;
    private String fileName;
    private JobType jobType;
    private String nodeGuid;
    private String versionName;
    private String startStep;
    private String endStep;
    private boolean ignoreCheck = true;
    private List<String> objectives = new ArrayList<>();
    private String releaseDateStr;
    private String snapshotDateStr;
    private String sourcePath;
    private boolean backupApplication = false;
    private String backupName;

    private JobRequestBuilder(String appGuid, String sourcePath, JobType jobType) {
        this.appGuid = appGuid;
        this.sourcePath = sourcePath;
        this.jobType = jobType;
        this.startStep = Constants.EXTRACT_STEP_NAME;
        this.endStep = Constants.CONSOLIDATE_SNAPSHOT;
        this.objectives.add(GLOBAL_RISK_OBJECTIVE);
        this.objectives.add(FUNCTIONAL_POINTS_OBJECTIVE);

        Date now = new Date();
        this.versionName = String.format("v%s", VERSION_NAME_FORMATTER.format(now));
        String nowStr = RELEASE_DATE_FORMATTER.format(now);
        this.releaseDateStr = nowStr;
        this.snapshotDateStr = nowStr;
    }

    public JobRequestBuilder nodeGuid(String nodeGuid) {
        this.nodeGuid = nodeGuid;
        return this;
    }

    public JobRequestBuilder versionName(String versionName) {
        if (StringUtils.isNotBlank(versionName)) {
            this.versionName = versionName;
        }
        return this;
    }

    public JobRequestBuilder startStep(String startStep) {
        this.startStep = startStep;
        return this;
    }

    public JobRequestBuilder endStep(String endStep) {
        this.endStep = endStep;
        return this;
    }

    public JobRequestBuilder ignoreCheck(boolean ignoreCheck) {
        this.ignoreCheck = ignoreCheck;
        return this;
    }

    public JobRequestBuilder backupApplication(boolean backupApplication) {
        this.backupApplication = backupApplication;
        return this;
    }

    public JobRequestBuilder backupName(String backupName) {
        this.backupName = backupName;
        return this;
    }

    public JobRequestBuilder securityObjective(boolean enable) {
        if (enable) {
            this.objectives.add(SECURITY_OBJECTIVE);
        }
        if (!enable && this.objectives.contains(SECURITY_OBJECTIVE)) {
            this.objectives = new ArrayList<>();
            objectives.add(GLOBAL_RISK_OBJECTIVE);
            objectives.add(FUNCTIONAL_POINTS_OBJECTIVE);
        }
        return this;
    }

    public JobRequestBuilder releaseDateStr(String releaseDateStr) {
        this.releaseDateStr = releaseDateStr;
        return this;
    }

    public JobRequestBuilder releaseAndSnapshotDate(Date date) {
        if (date == null) {
            return this;
        }
        String dateStr = RELEASE_DATE_FORMATTER.format(date);
        return this.releaseDateStr(dateStr)
                .snapshotDateStr(dateStr);
    }

    public JobRequestBuilder snapshotDateStr(String snapshotDateStr) {
        this.snapshotDateStr = snapshotDateStr;
        return this;
    }

    private Map<String, String> getJobParameters() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.PARAM_APP_GUID, this.appGuid);
        parameters.put(Constants.PARAM_VERSION_NAME, this.versionName);
        if (StringUtils.isNotBlank(sourcePath)) {
            parameters.put(Constants.PARAM_SOURCE_PATH, sourcePath);
        }
        if (StringUtils.isNotBlank(nodeGuid)) {
            parameters.put(Constants.PARAM_NODE_GUID, nodeGuid);
        }
        parameters.put(Constants.PARAM_START_STEP, startStep);
        parameters.put(Constants.PARAM_END_STEP, endStep);
        parameters.put(Constants.PARAM_IGNORE_CHECK, Boolean.toString(ignoreCheck));
        parameters.put(Constants.PARAM_VERSION_OBJECTIVES, String.join(",", objectives));
        parameters.put(Constants.PARAM_RELEASE_DATE, releaseDateStr);
        if (StringUtils.isBlank(snapshotDateStr)) {
            parameters.put(Constants.PARAM_SNAPSHOT_CAPTURE_DATE, releaseDateStr);
        } else {
            parameters.put(Constants.PARAM_SNAPSHOT_CAPTURE_DATE, snapshotDateStr);
        }

        if (backupApplication) {
            parameters.put(Constants.PARAM_BACKUP_ENABLED, Boolean.toString(backupApplication));
            if (StringUtils.isBlank(backupName)) {
                backupName = "backup_" + VERSION_NAME_FORMATTER.format(new Date());
            }
            parameters.put(Constants.PARAM_BACKUP_NAME, backupName);
        }

        return parameters;
    }

    public CreateJobsRequest buildJobRequest() {
        CreateJobsRequest jobRequest = new CreateJobsRequest();
        jobRequest.setJobType(jobType);
        jobRequest.setJobParameters(this.getJobParameters());
        return jobRequest;
    }

    public static JobRequestBuilder newInstance(String appGuid, String sourcePath, JobType jobType) {
        return new JobRequestBuilder(appGuid, sourcePath, jobType);
    }
}
