package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.utils.Constants;
import hudson.model.Run;
import hudson.util.Secret;
import jenkins.model.RunAction2;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * Action for the Create Application step
 * Stores the application name
 */
public class CreateApplicationAction implements RunAction2 {
    private Run run;
    private String applicationName;
    private boolean failureIgnored = false;
    private boolean inPlaceMode = false;
    private long timeout = Constants.DEFAULT_HTTP_TIMEOUT;
    @Nullable
    private String nodeName;
    @Nullable
    private String domainName;
    @Nullable
    private String aipConsoleUrl;
    @Nullable
    private Secret apiKey;

    public void setApiKey(Secret apiKey) {
        this.apiKey = apiKey;
    }

    @Nullable
    public Secret getApiKey() {
        return apiKey;
    }

    public void setAipConsoleUrl(String aipConsoleUrl) {
        this.aipConsoleUrl = aipConsoleUrl;
    }

    @Nullable
    public String getAipConsoleUrl() {
        return aipConsoleUrl;
    }

    public Run getRun() {
        return run;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
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
        return Messages.CreateApplicationBuilder_DescriptorImpl_displayName();
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return null;
    }
}
