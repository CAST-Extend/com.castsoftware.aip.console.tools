package io.jenkins.plugins.aipconsole;

import hudson.util.Secret;

import javax.annotation.Nullable;

public abstract class BaseAction {
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
}
