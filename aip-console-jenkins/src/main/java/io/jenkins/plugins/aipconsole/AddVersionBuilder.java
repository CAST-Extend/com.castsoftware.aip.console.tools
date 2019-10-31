package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobStatus;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.UploadException;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.ChunkedUploadService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.google.inject.Guice;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.Secret;
import io.jenkins.plugins.aipconsole.config.AipConsoleGlobalConfiguration;
import jenkins.tasks.SimpleBuildStep;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_accessDenied;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_appCreateError;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_appNotFound;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_jobFailure;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_jobServiceException;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_missingRequiredParameters;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_noApiKey;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_noServerUrl;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_uploadFailed;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_info_appNotFoundAutoCreate;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_info_pollJobMessage;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_info_startAddVersionJob;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_info_startUpload;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_success_analysisComplete;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_DescriptorImpl_displayName;
import static io.jenkins.plugins.aipconsole.Messages.CreateApplicationBuilder_CreateApplication_error_jobServiceException;
import static io.jenkins.plugins.aipconsole.Messages.JobsSteps_changed;

public class AddVersionBuilder extends Builder implements SimpleBuildStep {

    @Inject
    private JobsService jobsService;

    @Inject
    private ChunkedUploadService chunkedUploadService;

    @Inject
    private RestApiService apiService;

    @Inject
    private ApplicationService applicationService;

    private String applicationName;
    private String applicationGuid;
    private String filePath;
    private boolean autoCreate = false;
    private boolean cloneVersion = false;
    @Nullable
    private String versionName = "";
    private long timeout = Constants.DEFAULT_HTTP_TIMEOUT;
    private boolean failureIgnored = false;

    @DataBoundConstructor
    public AddVersionBuilder(String applicationName, String filePath) {
        this.applicationName = applicationName;
        this.filePath = filePath;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationGuid() {
        return applicationGuid;
    }

    @DataBoundSetter
    public void setApplicationGuid(String applicationGuid) {
        this.applicationGuid = applicationGuid;
    }

    public String getFilePath() {
        return filePath;
    }

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

    public long getTimeout() {
        return timeout;
    }

    @DataBoundSetter
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public AddVersionDescriptorImpl getDescriptor() {
        return (AddVersionDescriptorImpl) super.getDescriptor();
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        PrintStream log = listener.getLogger();
        Result defaultResult = failureIgnored ? Result.UNSTABLE : Result.FAILURE;

        if (apiService == null || chunkedUploadService == null || jobsService == null) {
            Guice.createInjector(new AipConsoleModule()).injectMembers(this);
        }

        if (StringUtils.isAnyBlank(applicationName, filePath)) {
            listener.error(AddVersionBuilder_AddVersion_error_missingRequiredParameters());
            run.setResult(Result.ABORTED);
            return;
        }

        String apiServerUrl = getDescriptor().getAipConsoleUrl();
        String apiKey = Secret.toString(getDescriptor().getAipConsoleSecret());
        String username = getDescriptor().getAipConsoleUsername();
        long actualTimeout = (timeout != Constants.DEFAULT_HTTP_TIMEOUT ? timeout : getDescriptor().getTimeout());

        if (StringUtils.isBlank(apiServerUrl)) {
            listener.error(AddVersionBuilder_AddVersion_error_noServerUrl());
            run.setResult(Result.ABORTED);
            return;
        }
        if (StringUtils.isBlank(apiKey)) {
            listener.error(AddVersionBuilder_AddVersion_error_noApiKey());
            run.setResult(Result.ABORTED);
            return;
        }

        try {
            // update timeout of HTTP Client if different from default
            if (actualTimeout != Constants.DEFAULT_HTTP_TIMEOUT) {
                apiService.setTimeout(actualTimeout, TimeUnit.SECONDS);
            }
            // legacy basic auth
            if (StringUtils.isNotBlank(username)) {
                apiService.validateUrlAndKey(apiServerUrl, username, apiKey);
            } else {
                apiService.validateUrlAndKey(apiServerUrl, apiKey);
            }
        } catch (ApiCallException e) {
            listener.error(AddVersionBuilder_AddVersion_error_accessDenied(apiServerUrl));
            run.setResult(defaultResult);
            return;
        }
        EnvVars vars = run.getEnvironment(listener);
        String resolvedFilePath = vars.expand(filePath);
        FilePath uploadFile;

        try {

            // Get the GUID from AIP Console if it is blank/null
            if (StringUtils.isBlank(applicationGuid)) {
                applicationGuid = applicationService.getApplicationGuidFromName(applicationName);
            }

            // Check again for blank/null and check if you should create it
            if (StringUtils.isBlank(applicationGuid)) {
                if (!autoCreate) {
                    listener.error(AddVersionBuilder_AddVersion_error_appNotFound(applicationName));
                    run.setResult(defaultResult);
                    return;
                }
                log.println(AddVersionBuilder_AddVersion_info_appNotFoundAutoCreate(applicationName));
                String jobGuid = jobsService.startCreateApplication(applicationName);
                applicationGuid = jobsService.pollAndWaitForJobFinished(jobGuid,
                        jobStatusWithSteps -> log.println(JobsSteps_changed(JobStepTranslationHelper.getStepTranslation(jobStatusWithSteps.getProgressStep()))),
                        s -> s.getState() == JobState.COMPLETED ? s.getAppGuid() : null);
                if (StringUtils.isBlank(applicationGuid)) {
                    listener.error(CreateApplicationBuilder_CreateApplication_error_jobServiceException(applicationName, apiServerUrl));
                    run.setResult(defaultResult);
                    return;
                }
            }

            log.println(AddVersionBuilder_AddVersion_info_startUpload(FilenameUtils.getName(resolvedFilePath)));
            FilePath workspaceFile = workspace.child(resolvedFilePath);
            if (!workspaceFile.exists()) {

                log.println("File " + workspaceFile.getBaseName() + " doesnt exists");
                run.setResult(defaultResult);
                return;
            }
            // Rename the file to applicationName-versionName.ext
            String extension = FilenameUtils.getExtension(resolvedFilePath);
            if (StringUtils.endsWithIgnoreCase(resolvedFilePath, ".tar.gz")) {
                // getExtension only returns the last extension, so specific case for .tar.gz
                extension = ".tar.gz";
            }
            uploadFile = workspace.child(String.format("%s.%s", UUID.randomUUID().toString(), extension));
            // if it already exists, delete it (might be a remnant of a previous execution)
            // move source file to another file name, to avoid conflicts when uploading the same zip file for multiple applications
            workspaceFile.renameTo(uploadFile);
            try (InputStream workspaceFileStream = uploadFile.read();
                 InputStream bufferedStream = new BufferedInputStream(workspaceFileStream, 10 * 1024 * 1024)) {
                log.println("Uploading file " + uploadFile.getName());
                if (!chunkedUploadService.uploadInputStream(applicationGuid, uploadFile.getName(), uploadFile.length(), bufferedStream)) {
                    throw new UploadException("Uploading was not completed successfully.");
                }
                // Cleanup the workspace
                uploadFile.delete();
            }

        } catch (ApplicationServiceException e) {
            listener.error(AddVersionBuilder_AddVersion_error_appCreateError(applicationName));
            e.printStackTrace(listener.getLogger());
            run.setResult(defaultResult);
            return;
        } catch (UploadException e) {
            listener.error(AddVersionBuilder_AddVersion_error_uploadFailed());
            e.printStackTrace(listener.getLogger());
            run.setResult(defaultResult);
            return;
        } catch (JobServiceException e) {
            listener.error(CreateApplicationBuilder_CreateApplication_error_jobServiceException(applicationName, apiServerUrl));
            e.printStackTrace(listener.getLogger());
            run.setResult(defaultResult);
            return;
        }

        try {
            // Create a value for versionName
            String resolvedVersionName = vars.expand(versionName);

            if (StringUtils.isBlank(resolvedVersionName)) {
                DateFormat formatVersionName = new SimpleDateFormat("yyMMdd.HHmmss");
                resolvedVersionName = String.format("v%s", formatVersionName.format(new Date()));
            }

            log.println(AddVersionBuilder_AddVersion_info_startAddVersionJob(applicationName));
            String jobGuid = jobsService.startAddVersionJob(applicationGuid, uploadFile.getName(), resolvedVersionName, new Date(), this.cloneVersion);
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
            listener.error(AddVersionBuilder_AddVersion_error_jobServiceException());
            e.printStackTrace(listener.getLogger());
            run.setResult(defaultResult);
        }
    }

    private JobState pollJob(String jobGuid, PrintStream log) throws JobServiceException {
        return jobsService.pollAndWaitForJobFinished(jobGuid,
                jobStatusWithSteps -> log.println(
                        jobStatusWithSteps.getAppName() + " - " +
                                JobsSteps_changed(JobStepTranslationHelper.getStepTranslation(jobStatusWithSteps.getProgressStep()))
                ),
                JobStatus::getState);
    }

    @Symbol("aipAddVersion")
    @Extension
    public static final class AddVersionDescriptorImpl extends BuildStepDescriptor<Builder> {

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

        public String getAipConsoleUrl() {
            return configuration.getAipConsoleUrl();
        }

        public Secret getAipConsoleSecret() {
            return configuration.getApiKey();
        }

        public String getAipConsoleUsername() {
            return configuration.getUsername();
        }

        public int getTimeout() {
            return configuration.getTimeout();
        }
    }
}
