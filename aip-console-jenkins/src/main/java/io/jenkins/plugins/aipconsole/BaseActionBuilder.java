package io.jenkins.plugins.aipconsole;

import hudson.tasks.Builder;
import hudson.util.Secret;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class BaseActionBuilder extends Builder {
    @Nullable
    private String aipConsoleUrl;
    @Nullable
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
        return StringUtils.isEmpty(Secret.toString(getLocalApiKey())) ? getDescriptor().getAipConsoleSecret() : getLocalApiKey();
    }

    public Secret getLocalApiKey() {
        return apiKey;
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
