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

public class JobParametersBuilder {
    private static final DateFormat RELEASE_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final DateFormat VERSION_NAME_FORMATTER = new SimpleDateFormat("yyMMdd.HHmmss");
    private static final String GLOBAL_RISK_OBJECTIVE = "GLOBAL_RISK";
    private static final String FUNCTIONAL_POINTS_OBJECTIVE = "FUNCTIONAL_POINTS";
    private static final String SECURITY_OBJECTIVE = "SECURITY";

    private String appGuid;
    private String fileName;
    private String nodeGuid;
    private String versionName;
    private String startStep;
    private String endStep;
    private boolean ignoreCheck = true;
    private List<String> objectives = new ArrayList<>();
    private String releaseDateStr;
    private String snapshotDateStr;
    private String sourceFolder;
    private String sourcePath;

    private JobParametersBuilder(String appGuid, String fileName) {
        this.appGuid = appGuid;
        this.fileName = fileName;
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

    public JobParametersBuilder nodeGuid(String nodeGuid) {
        this.nodeGuid = nodeGuid;
        return this;
    }

    public JobParametersBuilder versionName(String versionName) {
        if (StringUtils.isNotBlank(versionName)) {
            this.versionName = versionName;
        }
        return this;
    }

    public JobParametersBuilder startStep(String startStep) {
        this.startStep = startStep;
        return this;
    }

    public JobParametersBuilder endStep(String endStep) {
        this.endStep = endStep;
        return this;
    }

    public JobParametersBuilder ignoreCheck(boolean ignoreCheck) {
        this.ignoreCheck = ignoreCheck;
        return this;
    }

    public JobParametersBuilder securityObjective(boolean enable) {
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

    public JobParametersBuilder releaseDateStr(String releaseDateStr) {
        this.releaseDateStr = releaseDateStr;
        return this;
    }

    public JobParametersBuilder releaseAndSnapshotDate(Date date) {
        if (date == null) {
            return this;
        }
        String dateStr = RELEASE_DATE_FORMATTER.format(date);
        return this.releaseDateStr(dateStr)
                .snapshotDateStr(dateStr);
    }

    public JobParametersBuilder sourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
        return this;
    }

    public JobParametersBuilder snapshotDateStr(String snapshotDateStr) {
        this.snapshotDateStr = snapshotDateStr;
        return this;
    }

    public JobParametersBuilder sourceFolder(String applicationName) {
        if (StringUtils.isNotBlank(applicationName)) {
            this.sourceFolder = applicationName + "/main_sources";
        }
        return this;
    }

    public Map<String, String> build() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.PARAM_APP_GUID, this.appGuid);
        // parameters.put(Constants.PARAM_SOURC7E_ARCHIVE, this.fileName);
        //parameters.put(Constants.PARAM_FILENAME, this.fileName);
        parameters.put(Constants.PARAM_VERSION_NAME, this.versionName);
        if (StringUtils.isNotBlank(this.nodeGuid)) {
            parameters.put(Constants.PARAM_NODE_GUID, this.nodeGuid);
        }
        if (StringUtils.isNotBlank(this.sourceFolder)) {
            parameters.put(Constants.PARAM_SOURCE_FOLDER, this.sourceFolder);
        }
        parameters.put(Constants.PARAM_START_STEP, this.startStep);
        parameters.put(Constants.PARAM_END_STEP, this.endStep);
        parameters.put(Constants.PARAM_IGNORE_CHECK, Boolean.toString(this.ignoreCheck));
        parameters.put(Constants.PARAM_VERSION_OBJECTIVES, String.join(",", this.objectives));
        parameters.put(Constants.PARAM_RELEASE_DATE, this.releaseDateStr);
        parameters.put(Constants.PARAM_SNAPSHOT_CAPTURE_DATE, this.snapshotDateStr);
        parameters.put(Constants.PARAM_SOURCE_PATH, this.sourcePath);

        return parameters;
    }

    public CreateJobsRequest buildJobRequestWithParameters(JobType jobType) {
        CreateJobsRequest jobRequest = new CreateJobsRequest();
        jobRequest.setJobType(jobType);
        jobRequest.setJobParameters(this.build());
        return jobRequest;
    }

    public static JobParametersBuilder newInstance(String appGuid, String fileName) {
        return new JobParametersBuilder(appGuid, fileName);
    }
}
