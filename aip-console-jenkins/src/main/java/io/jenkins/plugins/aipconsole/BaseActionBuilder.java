package io.jenkins.plugins.aipconsole;

import hudson.tasks.Builder;
import hudson.util.Secret;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.CheckForNull;

public abstract class BaseActionBuilder extends Builder {
    private String aipConsoleUrl;
    private Secret apiKey;

    @Override
    public BaseActionBuilderDescriptor getDescriptor() {
        return (BaseActionBuilderDescriptor) super.getDescriptor();
    }

    @DataBoundSetter
    public void setApiKey(Secret apiKey) {
        this.apiKey = apiKey;
    }

    @CheckForNull
    public Secret getApiKey() {
        return apiKey == null ? getDescriptor().getAipConsoleSecret() : apiKey;
    }

    @DataBoundSetter
    public void setAipConsoleUrl(String aipConsoleUrl) {
        this.aipConsoleUrl = aipConsoleUrl;
    }

    @CheckForNull
    public String getAipConsoleUrl() {
        return StringUtils.isEmpty(aipConsoleUrl) ? getDescriptor().getAipConsoleUrl() : aipConsoleUrl;
    }
}
