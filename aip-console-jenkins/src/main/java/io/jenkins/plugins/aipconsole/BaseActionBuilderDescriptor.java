package io.jenkins.plugins.aipconsole;

import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.Secret;

public abstract class BaseActionBuilderDescriptor extends BuildStepDescriptor<Builder> {
    @Override
    public abstract String getDisplayName();

    public abstract String getAipConsoleUrl();

    public abstract Secret getAipConsoleSecret();

    public abstract String getAipConsoleUsername();

    public abstract int getTimeout();
}
