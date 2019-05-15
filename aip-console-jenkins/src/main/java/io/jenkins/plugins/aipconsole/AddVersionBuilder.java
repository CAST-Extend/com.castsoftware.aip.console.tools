package io.jenkins.plugins.aipconsole;

import com.castsoftware.uc.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.uc.aip.console.tools.core.dto.jobs.JobStatus;
import com.castsoftware.uc.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.uc.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.uc.aip.console.tools.core.exceptions.UploadException;
import com.castsoftware.uc.aip.console.tools.core.services.ChunkedUploadService;
import com.castsoftware.uc.aip.console.tools.core.services.JobsService;
import com.castsoftware.uc.aip.console.tools.core.services.RestApiService;
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
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static io.jenkins.plugins.aipconsole.Messages.*;

public class AddVersionBuilder extends Builder implements SimpleBuildStep {


    @Inject
    private JobsService jobsService;

    @Inject
    private ChunkedUploadService chunkedUploadService;

    @Inject
    private RestApiService apiService;

    private String applicationGuid;
    private String filePath;
    private boolean cloneVersion = false;
    @Nullable
    private String versionName = "";

    @DataBoundConstructor
    public AddVersionBuilder(String applicationGuid, String filePath) {
        this.applicationGuid = applicationGuid;
        this.filePath = filePath;
    }

    public String getApplicationGuid() {
        return applicationGuid;
    }

    public void setApplicationGuid(String applicationGuid) {
        this.applicationGuid = applicationGuid;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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

    @Override
    public AddVersionDescriptorImpl getDescriptor() {
        return (AddVersionDescriptorImpl) super.getDescriptor();
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        PrintStream log = listener.getLogger();

        if (apiService == null || chunkedUploadService == null || jobsService == null) {
            Guice.createInjector(new AipConsoleModule()).injectMembers(this);
        }

        if (StringUtils.isAnyBlank(applicationGuid, filePath)) {
            listener.error(AddVersionBuilder_AddVersion_error_missingRequiredParameters());
            run.setResult(Result.ABORTED);
            return;
        }

        String apiServerUrl = getDescriptor().getAipConsoleUrl();
        String apiKey = Secret.toString(getDescriptor().getAipConsoleSecret());
        String username = getDescriptor().getAipConsoleUsername();

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
            // legacy basic auth
            if (StringUtils.isNotBlank(username)) {
                apiService.validateUrlAndKey(apiServerUrl, username, apiKey);
            } else {
                apiService.validateUrlAndKey(apiServerUrl, apiKey);
            }
        } catch (ApiCallException e) {
            listener.error(AddVersionBuilder_AddVersion_error_accessDenied(apiServerUrl));
            run.setResult(Result.FAILURE);
            return;
        }
        EnvVars vars = run.getEnvironment(listener);

        try {
            String resolvedFilePath = vars.expand(filePath);
            log.println(AddVersionBuilder_AddVersion_info_startUpload());
            chunkedUploadService.uploadFile(this.applicationGuid, workspace.child(resolvedFilePath).getRemote());

        } catch (UploadException e) {
            listener.error(AddVersionBuilder_AddVersion_error_uploadFailed());
            e.printStackTrace(listener.getLogger());
            run.setResult(Result.FAILURE);
            return;
        }

        try {
            // Create a value for versionName
            String resolvedVersionName = vars.expand(versionName);

            if(StringUtils.isBlank(resolvedVersionName)) {
                DateFormat formatVersionName = new SimpleDateFormat("yyMMdd.HHmmss");
                resolvedVersionName = String.format("v%s", formatVersionName.format(new Date()));
            }

            log.println(AddVersionBuilder_AddVersion_info_startAddVersionJob());
            String jobGuid = jobsService.startAddVersionJob(applicationGuid, FilenameUtils.getName(filePath), resolvedVersionName, new Date(), this.cloneVersion);
            log.println(AddVersionBuilder_AddVersion_info_pollJobMessage());
            JobState state = jobsService.pollAndWaitForJobFinished(jobGuid,
                    jobStatusWithSteps -> log.println(JobsSteps_changed(JobStepTranslationHelper.getStepTranslation(jobStatusWithSteps.getProgressStep()))),
                    JobStatus::getState);
            if (state != JobState.COMPLETED) {
                listener.error(AddVersionBuilder_AddVersion_error_jobFailure(state.toString()));
                run.setResult(Result.FAILURE);
            } else {
                log.println(AddVersionBuilder_AddVersion_success_analysisComplete());
                run.setResult(Result.SUCCESS);
            }
        } catch (JobServiceException e) {
            listener.error(AddVersionBuilder_AddVersion_error_jobServiceException());
            e.printStackTrace(listener.getLogger());
            run.setResult(Result.FAILURE);
        }
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
    }
}
