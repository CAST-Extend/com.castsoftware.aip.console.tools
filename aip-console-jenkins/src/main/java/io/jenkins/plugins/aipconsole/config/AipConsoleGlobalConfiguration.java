package io.jenkins.plugins.aipconsole.config;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

@Extension
public class AipConsoleGlobalConfiguration extends GlobalConfiguration {

    private String aipConsoleUrl;
    private Secret apiKey;
    private String username;
    private int timeout = 30;

    public static AipConsoleGlobalConfiguration get() {
        return GlobalConfiguration.all().get(AipConsoleGlobalConfiguration.class);
    }

    public AipConsoleGlobalConfiguration(){
        load();
    }

    public String getAipConsoleUrl() {
        return aipConsoleUrl;
    }

    @DataBoundSetter
    public void setAipConsoleUrl(String aipConsoleUrl) {
        this.aipConsoleUrl = aipConsoleUrl;
        save();
    }

    public Secret getApiKey() {
        return apiKey;
    }

    @DataBoundSetter
    public void setApiKey(Secret apiKey) {
        this.apiKey = apiKey;
        save();
    }

    public String getUsername() {
        return username;
    }

    @DataBoundSetter
    public void setUsername(String username) {
        this.username = username;
        save();
    }

    public int getTimeout() {
        return timeout;
    }

    @DataBoundSetter
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public FormValidation doCheckAipConsoleUrl(@QueryParameter String value) {
        if(StringUtils.isBlank(value)) {
            return FormValidation.error("AIP Console URL cannot be empty");
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckApiKey(@QueryParameter String value) {
        if(StringUtils.isBlank(value)) {
            return FormValidation.error("AIP Console URL cannot be empty");
        }
        return FormValidation.ok();
    }
}
