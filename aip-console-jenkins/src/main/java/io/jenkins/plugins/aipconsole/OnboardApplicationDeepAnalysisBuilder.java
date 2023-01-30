package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobExecutionDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.LogContentDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.LogPollingProvider;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.utils.VersionInformation;
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
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_jobFailure;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_success_analysisComplete;
import static io.jenkins.plugins.aipconsole.Messages.GenericError_error_missingRequiredParameters;
import static io.jenkins.plugins.aipconsole.Messages.JobsSteps_changed;
import static io.jenkins.plugins.aipconsole.Messages.OnboardApplicationDeepAnalysisBuilder_DescriptorImpl_displayName;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_FastScanRequired;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_feature_notCompatible;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_applicationLookup;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_mode;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_runAnalysis_disabled;

public class OnboardApplicationDeepAnalysisBuilder extends CommonActionBuilder {
    final static boolean runAnalysis = true;
    @Nullable
    private String snapshotName;

    @Override
    protected String checkJobParameters() {
        if (StringUtils.isBlank(getApplicationName())) {
            return GenericError_error_missingRequiredParameters();
        }
        return super.checkJobParameters();
    }

    class JnksLogPollingProviderImpl implements LogPollingProvider {
        private final PrintStream log;
        private final boolean verbose;
        private Run<?, ?> run;
        private final TaskListener listener;

        JnksLogPollingProviderImpl(Run<?, ?> run, TaskListener listener, boolean verbose) {
            this.run = run;
            this.listener = listener;
            this.log = listener.getLogger();
            this.verbose = verbose;
        }

        @Override
        public String pollJobLog(String jobGuid) throws JobServiceException {
            JobExecutionDto jobExecutionDto = jobsService.pollAndWaitForJobFinished(jobGuid,
                    jobStatusWithSteps -> log.println(JobsSteps_changed(JobStepTranslationHelper.getStepTranslation(jobStatusWithSteps.getCurrentStep()))),
                    getPollingCallback(log), Function.identity(), TimeUnit.SECONDS.toMillis(2));
            //s -> s.getState() == JobState.COMPLETED ? s : null);

            if (jobExecutionDto.getState() != JobState.COMPLETED) {
                listener.error(AddVersionBuilder_AddVersion_error_jobFailure(jobExecutionDto.getState().toString()));
                run.setResult(getDefaultResult());
                return null;
            } else {
                log.println(AddVersionBuilder_AddVersion_success_analysisComplete());
                run.setResult(Result.SUCCESS);
                return jobExecutionDto.getGuid();
            }
        }

        private Consumer<LogContentDto> getPollingCallback(PrintStream log) {
            return !verbose ? null :
                    logContentDto -> {
                        logContentDto.getLines().forEach(logLine -> log.println(logLine.getContent()));
                    };
        }

    }

    @DataBoundConstructor
    public OnboardApplicationDeepAnalysisBuilder(String applicationName) {
        setApplicationName(applicationName);
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        super.perform(run, filePath, launcher, listener);

        String apiVersion = applicationService.getAipConsoleApiInfo().getApiVersion();
        if (getMinVersion() != null && StringUtils.isNotEmpty(apiVersion)) {
            VersionInformation serverApiVersion = VersionInformation.fromVersionString(apiVersion);
            if (serverApiVersion != null && getMinVersion().isHigherThan(serverApiVersion)) {
                listener.error(OnbordingApplicationBuilder_DescriptorImpl_feature_notCompatible("Onboard Application", apiVersion, getMinVersion().toString()));
                run.setResult(Result.FAILURE);
                return;
            }
        }

        String expandedAppName = environmentVariables.expand(getApplicationName());
        try {
            boolean OnBoardingModeWasOn = applicationService.isOnboardingSettingsEnabled();
            if (!OnBoardingModeWasOn) {
                logger.println(OnbordingApplicationBuilder_DescriptorImpl_label_mode("OFF"));
                run.setResult(getDefaultResult());
                return;
            }

            logger.println(OnbordingApplicationBuilder_DescriptorImpl_label_applicationLookup(expandedAppName));
            String existingAppGuid = null;
            boolean firstScan = true;
            ApplicationDto app = applicationService.getApplicationFromName(expandedAppName);
            if (app != null) {
                existingAppGuid = app.getGuid();
                app = applicationService.getApplicationDetails(existingAppGuid);
                firstScan = app.getVersion() == null || StringUtils.isEmpty(app.getVersion().getGuid()) || !app.isOnboarded();
            }

            if (firstScan || app == null) {
                logger.println(OnbordingApplicationBuilder_DescriptorImpl_FastScanRequired());
                run.setResult(getDefaultResult());
                return;
            }

            //on-boarding
            String caipVersion = app.getCaipVersion();
            String targetNode = app.getTargetNode();
            boolean verbose = getDescriptor().configuration.isVerbose();
            OnboardApplicationDeepAnalysisBuilder.JnksLogPollingProviderImpl jnksLogPollingProvider = new JnksLogPollingProviderImpl(run, listener, verbose);

            //Run Analysis or Deep analysis
            if (!applicationService.isImagingAvailable()) {
                logger.println(OnbordingApplicationBuilder_DescriptorImpl_label_runAnalysis_disabled());
            } else {
                String expandedSsnapshotName = environmentVariables.expand(getSnapshotName());
                if (StringUtils.isEmpty(app.getSchemaPrefix())) {
                    applicationService.runFirstScanApplication(existingAppGuid, targetNode, caipVersion, expandedSsnapshotName, verbose, jnksLogPollingProvider);
                } else {
                    applicationService.runReScanApplication(existingAppGuid, targetNode, caipVersion, expandedSsnapshotName, verbose, jnksLogPollingProvider);
                }
            }
        } catch (ApplicationServiceException e) {
            e.printStackTrace(logger);
            run.setResult(getDefaultResult());
            return;
        }
    }

    private static VersionInformation getMinVersion() {
        //This version can be null if failed to convert from string
        return VersionInformation.fromVersionString("2.5.0");
    }

    public String getSnapshotName() {
        return snapshotName;
    }

    @DataBoundSetter
    public void setSnapshotName(@Nullable String snapshotName) {
        this.snapshotName = snapshotName;
    }

    @Override
    public OnboardApplicationDeepAnalysisBuilder.OnboardApplicationDescriptorImpl getDescriptor() {
        return (OnboardApplicationDeepAnalysisBuilder.OnboardApplicationDescriptorImpl) super.getDescriptor();
    }

    @Symbol("OnboardApplication")
    @Extension
    public static final class OnboardApplicationDescriptorImpl extends BaseActionBuilderDescriptor {
        @Inject
        private AipConsoleGlobalConfiguration configuration;

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return OnboardApplicationDeepAnalysisBuilder_DescriptorImpl_displayName();
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
