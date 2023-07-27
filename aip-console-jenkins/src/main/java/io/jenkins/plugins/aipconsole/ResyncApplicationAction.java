package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.utils.Constants;
import hudson.model.Run;
import jenkins.model.RunAction2;

import javax.annotation.CheckForNull;

/**
 * Action for the Upgrade Application step
 */
public class ResyncApplicationAction extends BaseAction implements RunAction2 {
    private Run run;
    private String appName;
    private boolean failureIgnored = false;
    private boolean inPlaceMode = false;
    private long timeout = Constants.DEFAULT_HTTP_TIMEOUT;

    public Run getRun() {
        return run;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public boolean isFailureIgnored() {
        return failureIgnored;
    }

    public void setFailureIgnored(boolean failureIgnored) {
        this.failureIgnored = failureIgnored;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public boolean isInPlaceMode() {
        return inPlaceMode;
    }

    public void setInPlaceMode(boolean inPlaceMode) {
        this.inPlaceMode = inPlaceMode;
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
        return Messages.UpgradeApplicationBuilder_DescriptorImpl_displayName();
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return null;
    }
}

