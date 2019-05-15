package io.jenkins.plugins.aipconsole;

import hudson.model.Run;
import io.jenkins.plugins.aipconsole.Messages;
import jenkins.model.RunAction2;

import javax.annotation.CheckForNull;

/**
 * Action for the Create Application step
 * Stores the application name
 */
public class CreateApplicationAction implements RunAction2 {
    private Run run;
    private String applicationName;

    public Run getRun() {
        return run;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
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
        return Messages.CreateApplicationBuilder_DescriptorImpl_displayName();
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return null;
    }
}
