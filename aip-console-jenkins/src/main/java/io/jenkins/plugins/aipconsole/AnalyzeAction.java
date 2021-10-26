package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.utils.Constants;
import hudson.model.Run;
import jenkins.model.RunAction2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class AnalyzeAction extends BaseAction implements RunAction2 {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyzeAction.class);

    @CheckForNull
    private String applicationName;
    @Nullable
    private String applicationGuid;
    @Nullable
    private String versionName;
    private boolean withSnapshot = false;
    private boolean failureIgnored = false;
    private long timeout = Constants.DEFAULT_HTTP_TIMEOUT;
    private boolean processImaging = false;
    private boolean noConsolidation = true;

    private transient Run run;

    public AnalyzeAction(@CheckForNull String applicationName) {
        this.applicationName = applicationName;
    }
    
    @CheckForNull
    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(@CheckForNull String applicationName) {
        this.applicationName = applicationName;
    }

    @Nullable
    public String getApplicationGuid() {
        return applicationGuid;
    }

    public void setApplicationGuid(@Nullable String applicationGuid) {
        this.applicationGuid = applicationGuid;
    }

    @Nullable
    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(@Nullable String versionName) {
        this.versionName = versionName;
    }

    public boolean isFailureIgnored() {
        return failureIgnored;
    }

    public void setFailureIgnored(boolean failureIgnored) {
        this.failureIgnored = failureIgnored;
    }

    public boolean isWithSnapshot() {
        return withSnapshot;
    }

    public void setWithSnapshot(boolean withSnapshot) {
        this.withSnapshot = withSnapshot;
    }

    public boolean isNoConsolidation() {
        return noConsolidation;
    }

    public void setNoConsolidation(boolean noConsolidation) {
        this.noConsolidation = noConsolidation;
    }

    public boolean isProcessImaging() {
        return processImaging;
    }

    public void setProcessImaging(boolean processImaging) {
        this.processImaging = processImaging;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
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
        return Messages.AnalyzeBuilder_DescriptorImpl_displayName();
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return null;
    }
}
