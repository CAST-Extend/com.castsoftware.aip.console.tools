package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.LogContentDto;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.core.utils.LogUtils;
import com.castsoftware.aip.console.tools.core.utils.VersionInformation;
import com.google.inject.Guice;
import com.google.inject.Inject;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.Secret;
import io.jenkins.plugins.aipconsole.config.AipConsoleGlobalConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.jvnet.localizer.ResourceBundleHolder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static io.jenkins.plugins.aipconsole.Messages.CreateApplicationBuilder_CreateApplication_error_emptyApiKey;
import static io.jenkins.plugins.aipconsole.Messages.CreateApplicationBuilder_CreateApplication_error_emptyUrl;
import static io.jenkins.plugins.aipconsole.Messages.CreateApplicationBuilder_CreateApplication_error_jobFailed;
import static io.jenkins.plugins.aipconsole.Messages.CreateApplicationBuilder_CreateApplication_error_jobServiceException;
import static io.jenkins.plugins.aipconsole.Messages.CreateApplicationBuilder_CreateApplication_error_unavailable;
import static io.jenkins.plugins.aipconsole.Messages.CreateApplicationBuilder_CreateApplication_info_cssInfo;
import static io.jenkins.plugins.aipconsole.Messages.CreateApplicationBuilder_CreateApplication_info_jobStarted;
import static io.jenkins.plugins.aipconsole.Messages.CreateApplicationBuilder_CreateApplication_info_startJob;
import static io.jenkins.plugins.aipconsole.Messages.CreateApplicationBuilder_CreateApplication_info_success;
import static io.jenkins.plugins.aipconsole.Messages.CreateApplicationBuilder_DescriptorImpl_displayName;
import static io.jenkins.plugins.aipconsole.Messages.JobsSteps_changed;

/**
 * Builder to run a "Create application" step.
 * It'll create a new application on AIP Console with the given name
 */
public class CreateApplicationBuilder extends CommonActionBuilder {

    // Holder for dynamic loading of step name translations
    private final static ResourceBundleHolder holder = ResourceBundleHolder.get(io.jenkins.plugins.aipconsole.Messages.class);
    private final static String MESSAGES_STEP_KEY_PREFIX = "CreateApplicationBuilder.jobSteps.";

    private String applicationName;

    private String applicationGuid;

    private boolean failureIgnored = false;

    private long timeout = Constants.DEFAULT_HTTP_TIMEOUT;

    @Nullable
    private String nodeName;

    @Nullable
    private String cssServerName;

    @Nullable
    private String domainName;

    private boolean inPlaceMode;

    @DataBoundConstructor
    public CreateApplicationBuilder(String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public String getApplicationName() {
        return applicationName;
    }

    public boolean isFailureIgnored() {
        return failureIgnored;
    }

    public boolean isInPlaceMode() {
        return inPlaceMode;
    }

    @DataBoundSetter
    public void setInPlaceMode(boolean inPlaceMode) {
        this.inPlaceMode = inPlaceMode;
    }

    @DataBoundSetter
    public void setFailureIgnored(boolean failureIgnored) {
        this.failureIgnored = failureIgnored;
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

    @Nullable
    public String getCssServerName() {
        return cssServerName;
    }

    @DataBoundSetter
    public void setCssServerName(@Nullable String cssServerName) {
        this.cssServerName = cssServerName;
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

    @Override
    protected VersionInformation getFeatureMinVersion() {
        return null;
    }

    @Override
    public CreateAppDescriptorImpl getDescriptor() {
        return (CreateAppDescriptorImpl) super.getDescriptor();
    }

    @Override
    public void performClient(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        PrintStream log = listener.getLogger();
        Result defaultResult = failureIgnored ? Result.UNSTABLE : Result.FAILURE;

        if (apiService == null || jobsService == null) {
            Guice.createInjector(new AipConsoleModule()).injectMembers(this);
        }

        String apiServerUrl = getAipConsoleUrl();
        String apiKey = Secret.toString(getApiKey());
        String username = getDescriptor().getAipConsoleUsername();
        long actualTimeout = timeout != Constants.DEFAULT_HTTP_TIMEOUT ? timeout : getDescriptor().getTimeout();

        if (StringUtils.isBlank(apiServerUrl)) {
            listener.error(CreateApplicationBuilder_CreateApplication_error_emptyUrl());
            run.setResult(Result.NOT_BUILT);
            return;
        }
        if (StringUtils.isBlank(apiKey)) {
            listener.error(CreateApplicationBuilder_CreateApplication_error_emptyApiKey());
            run.setResult(Result.NOT_BUILT);
            return;
        }

        String expandedAppName = run.getEnvironment(listener).expand(applicationName);
        String expandedDomainName = run.getEnvironment(listener).expand(domainName);
        String expandedNodeName = run.getEnvironment(listener).expand(nodeName);

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

            String expandedCssServerName = run.getEnvironment(listener).expand(cssServerName);
            log.println(CreateApplicationBuilder_CreateApplication_info_cssInfo(expandedAppName, StringUtils.isEmpty(expandedCssServerName) ? "default" : expandedCssServerName));

            log.println(CreateApplicationBuilder_CreateApplication_info_startJob());
            String createJobGuid = jobsService.startCreateApplication(expandedAppName, expandedNodeName, expandedDomainName, inPlaceMode, null, expandedCssServerName);
            log.println(CreateApplicationBuilder_CreateApplication_info_jobStarted());
            Consumer<LogContentDto> pollingCallback = (!getDescriptor().configuration.isVerbose()) ? null :
                    logContentDto -> {
                        logContentDto.getLines().forEach(logLine -> log.println(LogUtils.replaceAllSensitiveInformation(logLine.getContent())));
                    };

            JobState endState = jobsService.pollAndWaitForJobFinished(createJobGuid,
                    jobStatusWithSteps -> log.println(JobsSteps_changed(JobStepTranslationHelper.getStepTranslation(jobStatusWithSteps.getCurrentStep()))),
                    pollingCallback,
                    jobStatusWithSteps -> {
                        applicationGuid = jobStatusWithSteps.getAppGuid();
                        return jobStatusWithSteps.getState();
                    }, null);

            if (endState != JobState.COMPLETED) {
                listener.error(CreateApplicationBuilder_CreateApplication_error_jobFailed(endState.toString()));
                run.setResult(defaultResult);
            } else {
                log.println(CreateApplicationBuilder_CreateApplication_info_success(expandedAppName, applicationGuid));
                run.setResult(Result.SUCCESS);
            }
        } catch (JobServiceException e) {
            listener.error(CreateApplicationBuilder_CreateApplication_error_jobServiceException(expandedAppName, e.getLocalizedMessage()));
            run.setResult(defaultResult);
        } catch (ApiCallException e) {
            listener.error(CreateApplicationBuilder_CreateApplication_error_unavailable(apiServerUrl));
            run.setResult(defaultResult);
        }
    }

    @Symbol("aipCreateApp")
    @Extension
    public static final class CreateAppDescriptorImpl extends BaseActionBuilderDescriptor {

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
