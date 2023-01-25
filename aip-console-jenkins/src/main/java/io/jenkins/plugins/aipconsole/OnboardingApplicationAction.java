package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.utils.Constants;
import hudson.model.Run;
import jenkins.model.RunAction2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class OnboardingApplicationAction extends BaseAction implements RunAction2 {
    private static final Logger LOGGER = LoggerFactory.getLogger(OnboardingApplicationAction.class);
    @CheckForNull
    private String applicationName;
    @Nullable
    private String applicationGuid;
    @Nullable
    private String filePath;
    @Nullable
    private String nodeName;
    @Nullable
    private String domainName;
    @CheckForNull
    private String onboardStrategy = "FIRST_SCAN";

    @Nullable
    private String exclusionPatterns = "";
    private long timeout = Constants.DEFAULT_HTTP_TIMEOUT;

    private transient Run run;

    public OnboardingApplicationAction(@CheckForNull String applicationName, String filePath) {
        this.applicationName = applicationName;
        this.filePath = filePath;
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
    public String getApplicationGuid() {
        return applicationGuid;
    }

    public void setApplicationGuid(@Nullable String applicationGuid) {
        this.applicationGuid = applicationGuid;
    }
    @Nullable
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(@Nullable String filePath) {
        this.filePath = filePath;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Nullable
    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(@Nullable String nodeName) {
        this.nodeName = nodeName;
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return null;
    }

    @Nullable
    public String getExclusionPatterns() {
        return exclusionPatterns;
    }

    public void setExclusionPatterns(@Nullable String exclusionPatterns) {
        this.exclusionPatterns = exclusionPatterns;
    }

    @Nullable
    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(@Nullable String domainName) {
        this.domainName = domainName;
    }

    public void setOnboardStrategy(@CheckForNull String onboardStrategy) {
        this.onboardStrategy = onboardStrategy;
    }


    @CheckForNull
    public String getOnboardStrategy() {
        return onboardStrategy;
    }
}
