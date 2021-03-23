package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.utils.Constants;
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
    private boolean cloneVersion = true;

    @Nullable
    private String versionName;
    private boolean failureIgnored = false;
    private long timeout = Constants.DEFAULT_HTTP_TIMEOUT;
    @Nullable
    private String nodeName;
    private boolean backupApplicationEnabled = false;
    @Nullable
    private String backupName = "";
    @Nullable
    private String domainName;
    private boolean disableImaging = false;

    @Nullable
    private String snapshotName;

    private boolean inPlaceMode = false;

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

    @Nullable
    public String getSnapshotName() {
        return snapshotName;
    }

    public void setSnapshotName(@Nullable String snapshotName) {
        this.snapshotName = snapshotName;
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

    @Nullable
    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(@Nullable String nodeName) {
        this.nodeName = nodeName;
    }

    public boolean isBackupApplicationEnabled() {
        return backupApplicationEnabled;
    }

    public void setBackupApplicationEnabled(boolean backupApplicationEnabled) {
        this.backupApplicationEnabled = backupApplicationEnabled;
    }

    @Nullable
    public String getBackupName() {
        return backupName;
    }

    public void setBackupName(@Nullable String backupName) {
        this.backupName = backupName;
    }

    public boolean isDisableImaging() {
        return disableImaging;
    }

    public void setDisableImaging(boolean disableImaging) {
        this.disableImaging = disableImaging;
    }

    @Nullable
    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(@Nullable String domainName) {
        this.domainName = domainName;
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

    public boolean isInPlaceMode() {
        return inPlaceMode;
    }

    public void setInPlaceMode(boolean inPlaceMode) {
        this.inPlaceMode = inPlaceMode;
    }
}
