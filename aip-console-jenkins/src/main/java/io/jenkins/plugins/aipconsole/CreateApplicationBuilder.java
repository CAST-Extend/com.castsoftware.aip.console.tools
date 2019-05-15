package io.jenkins.plugins.aipconsole;

import com.castsoftware.uc.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.uc.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.uc.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.uc.aip.console.tools.core.services.JobsService;
import com.castsoftware.uc.aip.console.tools.core.services.RestApiService;
import com.google.inject.Guice;
import com.google.inject.Inject;
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
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.jvnet.localizer.ResourceBundleHolder;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;

import static io.jenkins.plugins.aipconsole.Messages.*;

/**
 * Builder to run a "Create application" step.
 * It'll create a new application on AIP Console with the given name
 */
public class CreateApplicationBuilder extends Builder implements SimpleBuildStep {

    // Holder for dynamic loading of step name translations
    private final static ResourceBundleHolder holder = ResourceBundleHolder.get(io.jenkins.plugins.aipconsole.Messages.class);
    private final static String MESSAGES_STEP_KEY_PREFIX = "CreateApplicationBuilder.jobSteps.";

    @Inject
    private RestApiService apiService;
    @Inject
    private JobsService jobsService;

    private String applicationName;

    private String applicationGuid;

    @DataBoundConstructor
    public CreateApplicationBuilder(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    @Override
    public CreateAppDescriptorImpl getDescriptor() {
        return (CreateAppDescriptorImpl) super.getDescriptor();
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        PrintStream log = listener.getLogger();

        if (apiService == null || jobsService == null) {
            Guice.createInjector(new AipConsoleModule()).injectMembers(this);
        }

        String apiServerUrl = getDescriptor().getAipConsoleUrl();
        String apiKey = Secret.toString(getDescriptor().getAipConsoleSecret());
        String username = getDescriptor().getAipConsoleUsername();
        if (StringUtils.isBlank(apiServerUrl)) {
            listener.error(CreateApplicationBuilder_CreateApplication_error_emptyUrl());
            run.setResult(Result.ABORTED);
            return;
        }
        if (StringUtils.isBlank(apiKey)) {
            listener.error(CreateApplicationBuilder_CreateApplication_error_emptyApiKey());
            run.setResult(Result.ABORTED);
            return;
        }

        String expandedAppName = run.getEnvironment(listener).expand(applicationName);

        try {
            // legacy basic auth
            if (StringUtils.isNotBlank(username)) {
                apiService.validateUrlAndKey(apiServerUrl, username, apiKey);
            } else {
                apiService.validateUrlAndKey(apiServerUrl, apiKey);
            }

            log.println(CreateApplicationBuilder_CreateApplication_info_startJob());
            String createJobGuid = jobsService.startCreateApplication(expandedAppName);
            log.println(CreateApplicationBuilder_CreateApplication_info_jobStarted());
            JobState endState = jobsService.pollAndWaitForJobFinished(createJobGuid,
                    jobStatusWithSteps -> log.println(JobsSteps_changed(JobStepTranslationHelper.getStepTranslation(jobStatusWithSteps.getProgressStep()))),
                    jobStatusWithSteps -> {
                        applicationGuid = jobStatusWithSteps.getAppGuid();
                        return jobStatusWithSteps.getState();
                    });

            if (endState != JobState.COMPLETED) {
                listener.error(CreateApplicationBuilder_CreateApplication_error_jobFailed(endState.toString()));
                run.setResult(Result.FAILURE);
            } else {
                log.println(CreateApplicationBuilder_CreateApplication_info_success(expandedAppName, applicationGuid));
                run.setResult(Result.SUCCESS);
            }
        } catch (JobServiceException e) {
            listener.error(CreateApplicationBuilder_CreateApplication_error_jobServiceException(expandedAppName, e.getLocalizedMessage()));
            run.setResult(Result.FAILURE);
        } catch (ApiCallException e) {
            listener.error(CreateApplicationBuilder_CreateApplication_error_unavailable(apiServerUrl));
            run.setResult(Result.FAILURE);
        }
    }

    @Symbol("aipCreateApp")
    @Extension
    public static final class CreateAppDescriptorImpl extends BuildStepDescriptor<Builder> {

        @Inject
        private AipConsoleGlobalConfiguration configuration;

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return CreateApplicationBuilder_DescriptorImpl_displayName();
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
