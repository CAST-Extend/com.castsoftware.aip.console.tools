package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.utils.Constants;
import hudson.model.Run;
import jenkins.model.RunAction2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;

public class OnboardApplicationDeepAnalysisAction extends BaseAction implements RunAction2 {
    private static final Logger LOGGER = LoggerFactory.getLogger(OnboardApplicationDeepAnalysisAction.class);
    @CheckForNull
    private String applicationName;
    private long timeout = Constants.DEFAULT_HTTP_TIMEOUT;

    private transient Run run;

    public OnboardApplicationDeepAnalysisAction(@CheckForNull String applicationName) {
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
        return Messages.OnbordingApplicationBuilder_DescriptorImpl_displayName();
    }

    @CheckForNull
    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(@CheckForNull String applicationName) {
        this.applicationName = applicationName;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }


    @CheckForNull
    @Override
    public String getUrlName() {
        return null;
    }
}
