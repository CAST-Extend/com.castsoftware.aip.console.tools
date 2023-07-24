package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.LogContentDto;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.core.utils.LogUtils;
import com.google.inject.Guice;
import com.google.inject.Injector;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.Secret;
import io.jenkins.plugins.aipconsole.config.AipConsoleGlobalConfiguration;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import static io.jenkins.plugins.aipconsole.Messages.JobsSteps_changed;
import static io.jenkins.plugins.aipconsole.Messages.ResyncApplicationBuilder_DescriptorImpl_displayName;
import static io.jenkins.plugins.aipconsole.Messages.ResyncApplicationBuilder_ResyncApplication_error_jobFailed;
import static io.jenkins.plugins.aipconsole.Messages.ResyncApplicationBuilder_ResyncApplication_error_jobServiceException;
import static io.jenkins.plugins.aipconsole.Messages.ResyncApplicationBuilder_ResyncApplication_error_unavailable;
import static io.jenkins.plugins.aipconsole.Messages.ResyncApplicationBuilder_ResyncApplication_info_jobStarted;
import static io.jenkins.plugins.aipconsole.Messages.ResyncApplicationBuilder_ResyncApplication_info_jobSuccess;

public class ResyncApplicationBuilder extends BaseActionBuilder implements SimpleBuildStep {
    private static final DateFormat RELEASE_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Inject
    private JobsService jobsService;

    @Inject
    private RestApiService apiService;

    @Inject
    private ApplicationService applicationService;

    private String appGuid;
    private boolean failureIgnored = false;
    private long timeout = Constants.DEFAULT_HTTP_TIMEOUT;

    @DataBoundConstructor
    public ResyncApplicationBuilder(@CheckForNull String appGuid) {
        this.appGuid = appGuid;
    }

    @CheckForNull
    public String getAppGuid() {
        return appGuid;
    }

    public void setAppGuid(@Nullable String appGuid) {
        this.appGuid = appGuid;
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
    public ResyncApplicationBuilder.ResyncApplicationBuilderImpl getDescriptor() {
        return (ResyncApplicationBuilder.ResyncApplicationBuilderImpl) super.getDescriptor();
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        PrintStream log = listener.getLogger();
        Result defaultResult = failureIgnored ? Result.UNSTABLE : Result.FAILURE;

        if (apiService == null || jobsService == null) {
            Guice.createInjector(new AipConsoleModule()).injectMembers(this);
        }

        String errorMessage;
        if ((errorMessage = checkJobParameters()) != null) {
            listener.error(errorMessage);
            run.setResult(Result.NOT_BUILT);
            return;
        }

        // Check the services have been properly initialized
        if (!ObjectUtils.allNotNull(apiService, jobsService, applicationService)) {
            // Manually setup Guice Injector using Module (Didn't find any way to make this automatically)
            Injector injector = Guice.createInjector(new AipConsoleModule());
            // Guice can automatically inject those, but then findbugs, not seeing the change,
            // will fail the build considering they will provoke an NPE
            // So, to avoid this, set them explicitly (if they were not set)
            apiService = injector.getInstance(RestApiService.class);
            jobsService = injector.getInstance(JobsService.class);
            applicationService = injector.getInstance(ApplicationService.class);
        }

        String apiServerUrl = getAipConsoleUrl();
        String apiKey = Secret.toString(getApiKey());
        String username = getDescriptor().getAipConsoleUsername();
        // Job level timeout different from default ? use it, else use the global config level timeout
        long actualTimeout = (timeout != Constants.DEFAULT_HTTP_TIMEOUT ? timeout : getDescriptor().getTimeout());

        try {
            // update timeout of HTTP Client if different from default
            if (actualTimeout != Constants.DEFAULT_HTTP_TIMEOUT) {
                apiService.setTimeout(actualTimeout, TimeUnit.SECONDS);
            }
            // Authentication (if username is null or empty, we'll authenticate with api key
            apiService.validateUrlAndKey(apiServerUrl, username, apiKey);
        } catch (ApiCallException e) {
            listener.error(Messages.GenericError_error_accessDenied(apiServerUrl));
            run.setResult(defaultResult);
            return;
        }
        EnvVars vars = run.getEnvironment(listener);
        String expandedAppGuid = vars.expand(appGuid);

        String resyncJobGuid;
        String appName = null;

        try {
            ApplicationDto app = applicationService.getApplicationFromGuid(expandedAppGuid);
            appName = app.getName();

            String nodeCaipVersion = applicationService.getAipConsoleApiInfo().getCaipVersion();
            String appCaipVersion = app.getCaipVersion();

            log.println(String.format("Caip version of app: %s" , appCaipVersion));
            log.println(String.format("Caip version of node: %s", nodeCaipVersion));

            resyncJobGuid = jobsService.startResyncApplication(expandedAppGuid);

            log.println(ResyncApplicationBuilder_ResyncApplication_info_jobStarted());

            Consumer<LogContentDto> pollingCallback = (!getDescriptor().configuration.isVerbose()) ? null :
                    logContentDto -> logContentDto.getLines().forEach(logLine -> log.println(LogUtils.replaceAllSensitiveInformation(logLine.getContent())));

            JobState endState = jobsService.pollAndWaitForJobFinished(resyncJobGuid,
                    jobStatusWithSteps -> log.println(JobsSteps_changed(JobStepTranslationHelper.getStepTranslation(jobStatusWithSteps.getCurrentStep()))),
                    pollingCallback,
                    jobStatusWithSteps -> jobStatusWithSteps.getState(), null);

            if (endState != JobState.COMPLETED) {
                listener.error(ResyncApplicationBuilder_ResyncApplication_error_jobFailed(endState.toString()));
                run.setResult(defaultResult);
            } else {
                log.println(ResyncApplicationBuilder_ResyncApplication_info_jobSuccess(appName));
                run.setResult(Result.SUCCESS);
            }
        } catch (JobServiceException e) {
            listener.error(ResyncApplicationBuilder_ResyncApplication_error_jobServiceException(appName, e.getLocalizedMessage()));
            run.setResult(defaultResult);
        } catch (ApplicationServiceException e) {
            listener.error(ResyncApplicationBuilder_ResyncApplication_error_unavailable(apiServerUrl));
            run.setResult(defaultResult);
        }
    }

    /**
     * Check some initial elements before running the Job
     *
     * @return The error message based on the issue that was found, null if no issue was found
     */
    private String checkJobParameters() {
        if (StringUtils.isAnyBlank(appGuid)) {
            return Messages.GenericError_error_missingRequiredParameters();
        }
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

    @Symbol("aipResyncApp")
    @Extension
    public static final class ResyncApplicationBuilderImpl extends BaseActionBuilderDescriptor {

        @Inject
        private AipConsoleGlobalConfiguration configuration;

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return ResyncApplicationBuilder_DescriptorImpl_displayName();
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
