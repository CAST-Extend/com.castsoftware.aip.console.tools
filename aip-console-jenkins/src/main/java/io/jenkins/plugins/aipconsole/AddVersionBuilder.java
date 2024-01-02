package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.Exclusions;
import com.castsoftware.aip.console.tools.core.dto.ModuleGenerationType;
import com.castsoftware.aip.console.tools.core.dto.jobs.FileCommandRequest;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobExecutionDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobRequestBuilder;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobType;
import com.castsoftware.aip.console.tools.core.dto.jobs.LogContentDto;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.PackagePathInvalidException;
import com.castsoftware.aip.console.tools.core.exceptions.UploadException;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.core.utils.VersionInformation;
import com.castsoftware.aip.console.tools.core.utils.VersionObjective;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.Secret;
import io.jenkins.plugins.aipconsole.config.AipConsoleGlobalConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_appCreateError;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_appNotFound;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_fileNotFound;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_jobFailure;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_jobServiceException;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_uploadFailed;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_info_appNotFoundAutoCreate;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_info_noVersionAvailable;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_info_pollJobMessage;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_info_startAddVersionJob;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_info_startCloneVersionJob;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_info_startUpload;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_success_analysisComplete;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_DescriptorImpl_displayName;
import static io.jenkins.plugins.aipconsole.Messages.CreateApplicationBuilder_CreateApplication_error_jobServiceException;
import static io.jenkins.plugins.aipconsole.Messages.CreateApplicationBuilder_CreateApplication_info_cssInfo;
import static io.jenkins.plugins.aipconsole.Messages.JobsSteps_changed;
import static io.jenkins.plugins.aipconsole.Messages.Settings_Option_Dataflow_info;

public class AddVersionBuilder extends CommonActionBuilder {

    public static final int BUFFER_SIZE = 10 * 1024 * 1024;

    private String applicationName;
    private String applicationGuid;
    private String filePath;
    private boolean autoCreate = false;
    private String cssServerName;
    private boolean cloneVersion = true;
    private boolean blueprint = false;

    @Nullable
    private String versionName = "";
    private long timeout = Constants.DEFAULT_HTTP_TIMEOUT;
    private boolean failureIgnored = false;
    @Nullable
    private String nodeName = "";
    private boolean securityDataflow = false;
    private boolean enableDataSafety = false;

    private boolean backupApplicationEnabled = false;
    @Nullable
    private String backupName = "";
    @Nullable
    private String domainName;
    private boolean processImaging = false;
    private boolean consolidation = true;

    @Nullable
    private String snapshotName = "";

    private String moduleGenerationType;

    @DataBoundConstructor
    public AddVersionBuilder(String applicationName, String filePath) {
        this.applicationName = applicationName;
        this.filePath = filePath;
    }

    @Override
    public String getApplicationName() {
        return applicationName;
    }

    @Override
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public String getApplicationGuid() {
        return applicationGuid;
    }

    @Override
    @DataBoundSetter
    public void setApplicationGuid(String applicationGuid) {
        this.applicationGuid = applicationGuid;
    }

    @Override
    public String getFilePath() {
        return filePath;
    }

    @Override
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isAutoCreate() {
        return autoCreate;
    }

    @DataBoundSetter
    public void setAutoCreate(boolean autoCreate) {
        this.autoCreate = autoCreate;
    }

    @DataBoundSetter
    public void setBlueprint(boolean blueprint) {
        this.blueprint = blueprint;
    }

    public boolean getBlueprint() {
        return isBlueprint();
    }

    public boolean isBlueprint() {
        return blueprint;
    }

    public boolean isCloneVersion() {
        return cloneVersion;
    }

    @DataBoundSetter
    public void setCloneVersion(boolean cloneVersion) {
        this.cloneVersion = cloneVersion;
    }

    @Nullable
    public String getVersionName() {
        return versionName;
    }

    @DataBoundSetter
    public void setVersionName(@Nullable String versionName) {
        this.versionName = versionName;
    }

    public boolean isFailureIgnored() {
        return failureIgnored;
    }

    @DataBoundSetter
    public void setFailureIgnored(boolean failureIgnored) {
        this.failureIgnored = failureIgnored;
    }

    @DataBoundSetter
    public void setConsolidation(boolean consolidation) {
        this.consolidation = consolidation;
    }

    public boolean isConsolidation() {
        return consolidation;
    }

    @DataBoundSetter
    public void setModuleGenerationType(String moduleGenerationType) {
        this.moduleGenerationType = moduleGenerationType;
    }

    public String getModuleGenerationType() {
        return moduleGenerationType;
    }

    @Override
    public long getTimeout() {
        return timeout;
    }

    @Override
    @DataBoundSetter
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    @Nullable
    public String getNodeName() {
        return nodeName;
    }

    @Override
    @DataBoundSetter
    public void setNodeName(@Nullable String nodeName) {
        this.nodeName = nodeName;
    }

    public boolean isSecurityDataflow() {
        return securityDataflow;
    }

    @DataBoundSetter
    public void setSecurityDataflow(boolean securityDataflow) {
        this.securityDataflow = securityDataflow;
    }

    public boolean getSecurityDataflow() {
        return isSecurityDataflow();
    }

    @DataBoundSetter
    public void setEnableDataSafety(boolean enableDataSafety) {
        this.enableDataSafety = enableDataSafety;
    }

    public boolean isEnableDataSafety() {
        return enableDataSafety;
    }

    public boolean isBackupApplicationEnabled() {
        return backupApplicationEnabled;
    }

    @Nullable
    public String getBackupName() {
        return backupName;
    }

    @DataBoundSetter
    public void setBackupName(String backupName) {
        this.backupName = backupName;
    }

    @Nullable
    public String getSnapshotName() {
        return snapshotName;
    }

    @DataBoundSetter
    public void setSnapshotName(@Nullable String snapshotName) {
        this.snapshotName = snapshotName;
    }

    @DataBoundSetter
    public void setBackupApplicationEnabled(boolean backupApplicationEnabled) {
        this.backupApplicationEnabled = backupApplicationEnabled;
    }

    @Override
    @Nullable
    public String getDomainName() {
        return domainName;
    }

    @Override
    @DataBoundSetter
    public void setDomainName(@Nullable String domainName) {
        this.domainName = domainName;
    }

    public boolean isProcessImaging() {
        return processImaging;
    }

    @DataBoundSetter
    public void setProcessImaging(boolean processImaging) {
        this.processImaging = processImaging;
    }

    public String getCssServerName() {
        return cssServerName;
    }

    @DataBoundSetter
    public void setCssServerName(String cssServerName) {
        this.cssServerName = cssServerName;
    }

    @Override
    public AddVersionDescriptorImpl getDescriptor() {
        return (AddVersionDescriptorImpl) super.getDescriptor();
    }

    @Override
    protected void performClient(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        PrintStream log = listener.getLogger();
        Result defaultResult = failureIgnored ? Result.UNSTABLE : Result.FAILURE;
        boolean applicationHasVersion = cloneVersion;
        boolean isUpload = false;

        String errorMessage;
        if ((errorMessage = checkJobParameters()) != null) {
            listener.error(errorMessage);
            run.setResult(Result.NOT_BUILT);
            return;
        }

        // Parse variables in application name
        String variableAppName = environmentVariables.expand(applicationName);
        boolean inplaceMode = false;

        try {
            ApplicationDto app = applicationService.getApplicationFromName(variableAppName);
            inplaceMode = app == null ? false : app.isInPlaceMode();
            applicationGuid = app == null ? null : app.getGuid();
        } catch (ApplicationServiceException e) {
            listener.error(AddVersionBuilder_AddVersion_error_appCreateError(variableAppName));
            e.printStackTrace(listener.getLogger());
            run.setResult(defaultResult);
            return;
        }

        String resolvedFilePath = environmentVariables.expand(filePath);
        String fileExt = com.castsoftware.aip.console.tools.core.utils.FilenameUtils.getFileExtension(resolvedFilePath);
        FilePath workspaceFile = null;
        if (StringUtils.equalsAnyIgnoreCase(fileExt, "zip", "tgz", "tar.gz")) {
            workspaceFile = workspace.child(resolvedFilePath);
            isUpload = true;
            if (!workspaceFile.exists()) {
                listener.error(AddVersionBuilder_AddVersion_error_fileNotFound(resolvedFilePath));
                run.setResult(Result.NOT_BUILT);
                return;
            }
        }

        String fileName = UUID.randomUUID().toString();
        String caipVersion = null;
        ApplicationDto app = null;
        String apiServerUrl = getAipConsoleUrl();
        try {
            // Get the GUID from AIP Console
            if (StringUtils.isBlank(applicationGuid)) {
                if (!autoCreate) {
                    listener.error(AddVersionBuilder_AddVersion_error_appNotFound(variableAppName));
                    run.setResult(defaultResult);
                    return;
                }

                String expandedDomainName = environmentVariables.expand(domainName);
                String expandedNodeName = environmentVariables.expand(nodeName);
                String expandedCssServerName = run.getEnvironment(listener).expand(cssServerName);

                log.println(AddVersionBuilder_AddVersion_info_appNotFoundAutoCreate(variableAppName));
                log.println(CreateApplicationBuilder_CreateApplication_info_cssInfo(applicationName, cssServerName));
                String jobGuid = jobsService.startCreateApplication(variableAppName, expandedNodeName, expandedDomainName, inplaceMode, null, expandedCssServerName);
                applicationGuid = jobsService.pollAndWaitForJobFinished(jobGuid,
                        jobStatusWithSteps -> log.println(JobsSteps_changed(JobStepTranslationHelper.getStepTranslation(jobStatusWithSteps.getCurrentStep()))),
                        getPollingCallback(log),
                        s -> s.getState() == JobState.COMPLETED ? s.getAppGuid() : null, null);
                if (StringUtils.isBlank(applicationGuid)) {
                    listener.error(CreateApplicationBuilder_CreateApplication_error_jobServiceException(variableAppName, apiServerUrl));
                    run.setResult(defaultResult);
                    return;
                }
                // Don't clone version if we just created the application
                applicationHasVersion = false;
            }

            // If user asks for a "rescan" (i.e. clone previous version config)
            // check that there are versions on the application before launching the clone job
            if (applicationHasVersion) {
                applicationHasVersion = applicationService.applicationHasVersion(applicationGuid);
            }

            app = applicationService.getApplicationFromGuid(applicationGuid);
            caipVersion = app.getCaipVersion();

            if (!isUpload) {
                // Rename the file to applicationName-versionName.ext
                log.println(AddVersionBuilder_AddVersion_info_startUpload(FilenameUtils.getName(resolvedFilePath)));

                //call api to check if the folder exists
                try {
                    FileCommandRequest fileCommandRequest = FileCommandRequest.builder().command("LS").path("SOURCES:" + Paths.get(resolvedFilePath).toString()).build();
                    apiService.postForEntity("/api/server-folders", fileCommandRequest, String.class);
                } catch (ApiCallException e) {
                    listener.error("Unable to find the file " + resolvedFilePath + " in the source.folder.location on AIP Console.");
                    e.printStackTrace(log);
                    run.setResult(defaultResult);
                    return;
                }
                fileName = Paths.get(resolvedFilePath).toString();
                fileName = "sources:" + fileName;
            } else {
                fileName = String.format("%s.%s", fileName, fileExt);
                // if it already exists, delete it (might be a remnant of a previous execution)
                // move source file to another file name, to avoid conflicts when uploading the same zip file for multiple applications
                try (InputStream workspaceFileStream = workspaceFile.read();
                     InputStream bufferedStream = new BufferedInputStream(workspaceFileStream, BUFFER_SIZE)) {
                    log.println("Uploading file " + workspaceFile.getName());
                    if (!uploadService.uploadInputStream(applicationGuid, fileName, workspaceFile.length(), bufferedStream)) {
                        throw new UploadException("Uploading was not completed successfully.");
                    }

                    fileName = "upload:" + variableAppName + "/" + fileName;
                }
            }
        } catch (ApplicationServiceException e) {
            listener.error(AddVersionBuilder_AddVersion_error_appCreateError(variableAppName));
            e.printStackTrace(listener.getLogger());
            run.setResult(defaultResult);
            return;
        } catch (UploadException e) {
            listener.error(AddVersionBuilder_AddVersion_error_uploadFailed());
            e.printStackTrace(listener.getLogger());
            run.setResult(defaultResult);
            return;
        } catch (JobServiceException e) {
            listener.error(CreateApplicationBuilder_CreateApplication_error_jobServiceException(variableAppName, apiServerUrl));
            e.printStackTrace(listener.getLogger());
            run.setResult(defaultResult);
            return;
        }
        String jobGuid = null;
        try {
            // Create a value for versionName
            String resolvedVersionName = environmentVariables.expand(versionName);
            String resolvedSnapshotName = environmentVariables.expand(snapshotName);

            if (StringUtils.isBlank(resolvedVersionName)) {
                DateFormat formatVersionName = new SimpleDateFormat("yyMMdd.HHmmss");
                resolvedVersionName = String.format("v%s", formatVersionName.format(new Date()));
            }

            if (cloneVersion) {
                if (applicationHasVersion) {
                    log.println(AddVersionBuilder_AddVersion_info_startCloneVersionJob(variableAppName));
                } else {
                    log.println(AddVersionBuilder_AddVersion_info_noVersionAvailable(variableAppName));
                }
            } else {
                log.println(AddVersionBuilder_AddVersion_info_startAddVersionJob(variableAppName));
            }

            boolean expandedSecurityDataflow = isSecurityDataflowEnabled();
            JobRequestBuilder requestBuilder = JobRequestBuilder.newInstance(applicationGuid, fileName, applicationHasVersion ? JobType.CLONE_VERSION : JobType.ADD_VERSION, caipVersion)
                    .nodeName(app.getTargetNode())
                    .releaseAndSnapshotDate(new Date())
                    .versionName(resolvedVersionName)
                    .objectives(VersionObjective.DATA_SAFETY, isEnableDataSafety())
                    .objectives(VersionObjective.SECURITY, expandedSecurityDataflow)
                    .backupApplication(backupApplicationEnabled)
                    .backupName(backupName)
                    .processImaging(processImaging);

            String deliveryConfig = applicationService.createDeliveryConfiguration(applicationGuid, fileName, Exclusions.builder().build(), applicationHasVersion);
            if (StringUtils.isNotBlank(deliveryConfig)) {
                requestBuilder.deliveryConfigGuid(deliveryConfig);
            }

            if (StringUtils.isNotBlank(resolvedSnapshotName)) {
                requestBuilder.snapshotName(resolvedSnapshotName);
            }

            boolean forcedConsolidation = processImaging || consolidation;
            requestBuilder.uploadApplication(forcedConsolidation);
            if (!forcedConsolidation) {
                requestBuilder.endStep(Constants.SNAPSHOT_INDICATOR);
                log.println(String.format("The snapshot %s for application %s will be taken but will not be published.", resolvedSnapshotName, applicationName));
            } else if (processImaging) {
                requestBuilder.endStep(Constants.PROCESS_IMAGING);
            }

            if (StringUtils.isNotEmpty(moduleGenerationType)) {
                applicationService.updateModuleGenerationType(applicationGuid, requestBuilder, ModuleGenerationType.fromString(moduleGenerationType), !applicationHasVersion);
            }

            requestBuilder.objectives(VersionObjective.BLUEPRINT, isBlueprint());
            requestBuilder.objectives(VersionObjective.SECURITY, expandedSecurityDataflow);

            log.println(Settings_Option_Dataflow_info(expandedSecurityDataflow));
            applicationService.updateSecurityDataflow(applicationGuid, expandedSecurityDataflow, Constants.JEE_TECHNOLOGY_PATH);
            applicationService.updateSecurityDataflow(applicationGuid, expandedSecurityDataflow, Constants.DOTNET_TECHNOLOGY_PATH);

            log.println("Job request : " + requestBuilder.buildJobRequest().toString());
            jobGuid = jobsService.startAddVersionJob(requestBuilder);

            log.println(AddVersionBuilder_AddVersion_info_pollJobMessage());
            JobState state = pollJob(jobGuid, log);
            if (state != JobState.COMPLETED) {
                listener.error(AddVersionBuilder_AddVersion_error_jobFailure(state.toString()));
                run.setResult(defaultResult);
            } else {
                log.println(AddVersionBuilder_AddVersion_success_analysisComplete());
                run.setResult(Result.SUCCESS);
            }
        } catch (JobServiceException e) {
            // Should we check if the original cause is an InterruptedException and attempt to cancel the job ?
            if (e.getCause() != null && e.getCause() instanceof InterruptedException) {
                if (jobGuid != null) {
                    log.println("Attempting to cancel Analysis job on AIP Console, following cancellation of the build.");
                    run.setResult(Result.ABORTED);
                    try {
                        jobsService.cancelJob(jobGuid);
                        log.println("Job was successfully cancelled on AIP Console.");
                    } catch (JobServiceException jse) {
                        log.println("Could not cancel the job on AIP Console, please cancel it manually. Error was : " + e.getMessage());
                    }
                }
            } else {
                listener.error(AddVersionBuilder_AddVersion_error_jobServiceException());
                e.printStackTrace(listener.getLogger());
                run.setResult(defaultResult);
            }
        } catch (PackagePathInvalidException e) {
            log.println("Failed to match the package path(s) with the previous version, job will stop");
            listener.error(AddVersionBuilder_AddVersion_error_jobServiceException());
            e.printStackTrace(listener.getLogger());
            run.setResult(defaultResult);
        }
    }

    private boolean isSecurityDataflowEnabled() {
        return isSecurityDataflow() || Boolean.valueOf(environmentVariables.get("SECURITY_DATAFLOW"));
    }

    /**
     * Check some initial elements before running the Job
     *
     * @return The error message based on the issue that was found, null if no issue was found
     */
    @Override
    protected String checkJobParameters() {
        if (StringUtils.isAnyBlank(applicationName, filePath)) {
            return Messages.GenericError_error_missingRequiredParameters();
        }

        //Constraint annotation should have issued error if rule broken
        String apiServerUrl = getAipConsoleUrl();
        String apiKey = Secret.toString(getApiKey());

        if (StringUtils.isBlank(apiServerUrl)) {
            return Messages.GenericError_error_noServerUrl();
        }
        if (StringUtils.isBlank(apiKey)) {
            return Messages.GenericError_error_noApiKey();
        }

        return null;
    }

    @Override
    protected VersionInformation getFeatureMinVersion() {
        return null;
    }

    private Consumer<LogContentDto> getPollingCallback(PrintStream log) {
        return !getDescriptor().configuration.isVerbose() ? null :
                logContentDto -> {
                    logContentDto.getLines().forEach(logLine -> log.println(logLine.getContent()));
                };
    }

    private JobState pollJob(String jobGuid, PrintStream log) throws JobServiceException {
        return jobsService.pollAndWaitForJobFinished(jobGuid,
                jobStatusWithSteps -> log.println(
                        jobStatusWithSteps.getAppName() + " - " +
                                JobsSteps_changed(JobStepTranslationHelper.getStepTranslation(jobStatusWithSteps.getCurrentStep()))
                ),
                getPollingCallback(log),
                JobExecutionDto::getState, null);
    }

    @Symbol("aipAddVersion")
    @Extension
    public static final class AddVersionDescriptorImpl extends BaseActionBuilderDescriptor {

        @Inject
        private AipConsoleGlobalConfiguration configuration;

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return AddVersionBuilder_DescriptorImpl_displayName();
        }

        @Override
        public String getAipConsoleUrl() {
            return configuration.getAipConsoleUrl();
        }

        @Override
        public Secret getAipConsoleSecret() {
            return configuration.getApiKey();
        }

        @Override
        public String getAipConsoleUsername() {
            return configuration.getUsername();
        }

        @Override
        public int getTimeout() {
            return configuration.getTimeout();
        }
    }
}
