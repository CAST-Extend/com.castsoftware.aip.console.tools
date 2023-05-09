package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.utils.Constants;
import hudson.model.Run;
import jenkins.model.RunAction2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class OnboardApplicationDeepAnalysisAction extends BaseAction implements RunAction2 {
    private static final Logger LOGGER = LoggerFactory.getLogger(OnboardApplicationDeepAnalysisAction.class);
    @CheckForNull
    private String applicationName;
    @Nullable
    private String snapshotName;

    private String moduleGenerationType;

    private long timeout = Constants.DEFAULT_HTTP_TIMEOUT;

    private long sleepDuration = Constants.DEFAULT_SLEEP_DURATION;
    private transient Run run;

    public OnboardApplicationDeepAnalysisAction(@CheckForNull String applicationName) {
        this.applicationName = applicationName;
    }

    public String getModuleGenerationType() {
        return moduleGenerationType;
    }

    public void setModuleGenerationType(@CheckForNull String moduleGenerationType) {
        this.moduleGenerationType = moduleGenerationType;
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
        return Messages.OnbordingApplicationBuilder_DescriptorImpl_displayName();
    }

    @CheckForNull
    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(@CheckForNull String applicationName) {
        this.applicationName = applicationName;
    }

    @Nullable
    public String getSnapshotName() {
        return snapshotName;
    }

    public void setSnapshotName(@Nullable String snapshotName) {
        this.snapshotName = snapshotName;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public long getSleepDuration() {
        return sleepDuration;
    }

    public void setSleepDuration(long sleepDuration) {
        this.sleepDuration = sleepDuration;
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return null;
    }
}
