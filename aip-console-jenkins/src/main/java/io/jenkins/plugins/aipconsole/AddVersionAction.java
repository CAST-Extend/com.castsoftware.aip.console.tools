package io.jenkins.plugins.aipconsole;

import hudson.model.Run;
import jenkins.model.RunAction2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class AddVersionAction implements RunAction2 {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddVersionAction.class);

    @CheckForNull
    private String applicationName;
    @Nullable
    private String applicationGuid;
    @CheckForNull
    private String filePath;
    private boolean autoCreate = false;
    private boolean cloneVersion = false;
    @Nullable
    private String versionName;
    private transient Run run;

    public AddVersionAction(@CheckForNull String applicationName, @CheckForNull String filePath) {
        this.applicationName = applicationName;
        this.filePath = filePath;
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

    @CheckForNull
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(@CheckForNull String filePath) {
        this.filePath = filePath;
    }

    public boolean isAutoCreate() {
        return autoCreate;
    }

    public void setAutoCreate(boolean autoCreate) {
        this.autoCreate = autoCreate;
    }

    public boolean isCloneVersion() {
        return cloneVersion;
    }

    public void setCloneVersion(boolean cloneVersion) {
        this.cloneVersion = cloneVersion;
    }

    @Nullable
    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(@Nullable String versionName) {
        this.versionName = versionName;
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
        return Messages.AddVersionBuilder_DescriptorImpl_displayName();
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return null;
    }
}
