package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.utils.Constants;
import hudson.model.Run;
import jenkins.model.RunAction2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ArchitectureStudioAction extends BaseAction implements RunAction2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchitectureStudioAction.class);

    @CheckForNull
    private String applicationName;

    @CheckForNull
    private String modelName;

    @Nullable
    private String uploadFilePath;

    @Nullable
    private String reportPath;

    private final long timeout = Constants.DEFAULT_HTTP_TIMEOUT;

    private long sleepDuration = Constants.DEFAULT_SLEEP_DURATION;

    private transient Run run;

    public ArchitectureStudioAction(@CheckForNull String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public void onAttached(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        this.run = run;
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return Messages.ArchitectureStudioBuilder_DescriptorImpl_displayName();
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return null;
    }

    @CheckForNull
    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(@CheckForNull String applicationName) {
        this.applicationName = applicationName;
    }

    public String getModelName() { return modelName; }

    public void setModelName(@CheckForNull String modelName) { this.modelName = modelName; }

    public String getUploadFilePath() { return uploadFilePath; }

    public void setUploadFilePath(@Nullable String uploadFilePath) { this.uploadFilePath = uploadFilePath; }

    public String getReportPath() { return reportPath; }

    public void setReportPath(@Nullable String reportPath) { this.reportPath = reportPath; }

    public long getSleepDuration() {
        return sleepDuration;
    }

    public void setSleepDuration(long sleepDuration) {
        this.sleepDuration = sleepDuration;
    }
}
