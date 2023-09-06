package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationOnboardingDto;
import com.castsoftware.aip.console.tools.core.dto.DeliveryConfigurationDto;
import com.castsoftware.aip.console.tools.core.dto.Exclusions;
import com.castsoftware.aip.console.tools.core.dto.VersionStatus;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobExecutionDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.LogContentDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.LogPollingProvider;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.PackagePathInvalidException;
import com.castsoftware.aip.console.tools.core.exceptions.UploadException;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.utils.LogUtils;
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
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_jobFailure;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_success_analysisComplete;
import static io.jenkins.plugins.aipconsole.Messages.GenericError_error_missingRequiredParameters;
import static io.jenkins.plugins.aipconsole.Messages.JobsSteps_changed;
import static io.jenkins.plugins.aipconsole.Messages.JobsSteps_jobServiceException;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_FastScanForbidden;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_displayName;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_actionAboutToStart;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_actionDone;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_applicationLookup;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_deliveryConfiguration;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_deliveryConfiguration_done;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_mode;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_scanMode;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_upload;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_upload_done;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_upload_failed;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_missingFilePath;

public class OnboardApplicationFastScanBuilder extends CommonActionBuilder {
    private String applicationGuid;
    private String exclusionPatterns = "";
    private long sleepDuration;

    @Override
    protected String checkJobParameters() {
        if (StringUtils.isAnyBlank(getApplicationName(), getFilePath())) {
            return GenericError_error_missingRequiredParameters();
        }
        return super.checkJobParameters();
    }

    public long getSleepDuration() {
        return sleepDuration;
    }

    @DataBoundSetter
    public void setSleepDuration(long sleepDuration) {
        this.sleepDuration = sleepDuration;
    }

    static class JnksLogPollingProviderImpl implements LogPollingProvider {
        private final PrintStream log;
        private final boolean verbose;
        private Run<?, ?> run;
        private final TaskListener listener;
        private final JobsService jobsService;
        private final long sleepDuration;

        JnksLogPollingProviderImpl(JobsService jobsService, Run<?, ?> run, TaskListener listener, boolean verbose, long sleepDuration) {
            this.run = run;
            this.listener = listener;
            log = listener.getLogger();
            this.verbose = verbose;
            this.jobsService = jobsService;
            this.sleepDuration = sleepDuration;
        }

        @Override
        public String pollJobLog(String jobGuid) throws JobServiceException {
            JobExecutionDto jobExecutionDto = jobsService.pollAndWaitForJobFinished(jobGuid,
                    this::callbackFunction, getPollingCallback(log), Function.identity(), () -> TimeUnit.SECONDS.toMillis(sleepDuration));
            //s -> s.getState() == JobState.COMPLETED ? s : null);
            //JobExecutionDto jobExecutionDto = jobsService.pollAndWaitForJobFinished(jobGuid, this::callbackFunction, verbose);

            if (jobExecutionDto.getState() != JobState.COMPLETED) {
                listener.error(AddVersionBuilder_AddVersion_error_jobFailure(jobExecutionDto.getState().toString()));
                run.setResult(Result.FAILURE);
                return null;
            } else {
                log.println(AddVersionBuilder_AddVersion_success_analysisComplete());
                run.setResult(Result.SUCCESS);
                return jobExecutionDto.getGuid();
            }
        }

        private JobExecutionDto callbackFunction(JobExecutionDto jobExecutionDto) {
            //jobStatusWithSteps -> log.println(JobsSteps_changed(JobStepTranslationHelper.getStepTranslation(jobStatusWithSteps.getCurrentStep())))
            log.println(JobsSteps_changed(JobStepTranslationHelper.getStepTranslation(jobExecutionDto.getCurrentStep())));
            return jobExecutionDto;
        }

        private Consumer<LogContentDto> getPollingCallback(PrintStream log) {
            return !verbose ? null : logContentDto -> printLog(logContentDto);
        }

        private void printLog(LogContentDto logContent) {
            logContent.getLines().forEach(logLine -> log.println(LogUtils.replaceAllSensitiveInformation(logLine.getContent())));
        }
    }

    @DataBoundConstructor
    public OnboardApplicationFastScanBuilder(String applicationName, String filePath) {
        setApplicationName(applicationName);
        setFilePath(filePath);
    }

    @Override
    protected void performClient(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        String expandedAppName = environmentVariables.expand(getApplicationName());
        String expandedFilePath = environmentVariables.expand(getFilePath());
        String expandedDomainName = environmentVariables.expand(getDomainName());
        boolean runAnalysis = false;

        if (!runAnalysis && (StringUtils.isEmpty(expandedFilePath))) {
            logger.println(OnbordingApplicationBuilder_DescriptorImpl_missingFilePath());
            run.setResult(getDefaultResult());
            return;
        }

        try {
            if (!applicationService.isOnboardingSettingsEnabled()) {
                logger.println(OnbordingApplicationBuilder_DescriptorImpl_label_mode("OFF"));
                run.setResult(getDefaultResult());
                return;
            }

            logger.println(OnbordingApplicationBuilder_DescriptorImpl_label_applicationLookup(expandedAppName));

            ApplicationDto app = applicationService.getApplicationFromName(expandedAppName);

            String scanMode = " Fast-scan/Refresh";
            logger.println(OnbordingApplicationBuilder_DescriptorImpl_label_scanMode(expandedAppName + scanMode));

            boolean verbose = getDescriptor().configuration.isVerbose();
            applicationGuid = app == null ? null : app.getGuid();
            String uploadAction = StringUtils.isEmpty(applicationGuid) ? "onboard sources" : "refresh sources content";
            logger.println(OnbordingApplicationBuilder_DescriptorImpl_label_upload(uploadAction, expandedAppName));
            String sourcePath = uploadService.uploadFileForOnboarding(Paths.get(expandedFilePath).toFile(), applicationGuid);
            logger.println(OnbordingApplicationBuilder_DescriptorImpl_label_upload_done(uploadAction, sourcePath));

            if (app == null) {
                applicationGuid = applicationService.onboardApplication(expandedAppName, expandedDomainName, verbose, sourcePath);
            }

            //Refresh application information
            app = applicationService.getApplicationFromName(expandedAppName);
            if (!app.isOnboarded()) {
                logger.println(OnbordingApplicationBuilder_DescriptorImpl_FastScanForbidden());
                run.setResult(getDefaultResult());
                return;
            }

            applicationGuid = app.getGuid();

            ApplicationOnboardingDto applicationOnboardingDto = applicationService.getApplicationOnboarding(applicationGuid);
            String caipVersion = applicationOnboardingDto.getCaipVersion();
            String targetNode = applicationOnboardingDto.getTargetNode();

            //For RESCAN PROCESS: re-discover
            Exclusions exclusions = Exclusions.builder().excludePatterns(exclusionPatterns).build();

            //discover-packages
            logger.println(OnbordingApplicationBuilder_DescriptorImpl_label_deliveryConfiguration());
            DeliveryConfigurationDto[] deliveryConfig = new DeliveryConfigurationDto[1];
            String deliveryConfigurationGuid = applicationService.discoverPackagesAndCreateDeliveryConfiguration(applicationGuid, sourcePath, exclusions,
                    VersionStatus.DELIVERED, true, (config) -> deliveryConfig[0] = config, true);
            DeliveryConfigurationDto deliveryConfiguration = deliveryConfig[0];
            logger.println(OnbordingApplicationBuilder_DescriptorImpl_label_deliveryConfiguration_done(deliveryConfigurationGuid));
            deliveryConfiguration.setGuid(deliveryConfigurationGuid);
            //rediscover-application
            logger.println(OnbordingApplicationBuilder_DescriptorImpl_label_actionAboutToStart("Fast-Scan"));
            JnksLogPollingProviderImpl jnksLogPollingProvider = new JnksLogPollingProviderImpl(jobsService, run, listener, verbose, sleepDuration);
            String jobStatus = applicationService.fastScan(applicationGuid, sourcePath, "", deliveryConfiguration,
                    caipVersion, targetNode, verbose, jnksLogPollingProvider);
            if(jobStatus != null && jobStatus.equalsIgnoreCase(JobState.COMPLETED.toString())) {
                logger.println(OnbordingApplicationBuilder_DescriptorImpl_label_actionDone("Fast-Scan"));
            }
        } catch (ApplicationServiceException | JobServiceException e) {
            e.printStackTrace(logger);
            run.setResult(getDefaultResult());
            return;
        } catch (UploadException e) {
            listener.error(OnbordingApplicationBuilder_DescriptorImpl_label_upload_failed());
            e.printStackTrace(logger);
            run.setResult(getDefaultResult());
            return;
        } catch (PackagePathInvalidException e) {
            logger.println(JobsSteps_jobServiceException());
            listener.error(JobsSteps_jobServiceException());
            e.printStackTrace(logger);
            run.setResult(getDefaultResult());
            return;
        }
    }

    private static VersionInformation getMinVersion() {
        //This version can be null if failed to convert from string
        return VersionInformation.fromVersionString("2.8.0");
    }

    @Override
    protected VersionInformation getFeatureMinVersion() {
        return getMinVersion();
    }

    public String getExclusionPatterns() {
        return exclusionPatterns;
    }

    @DataBoundSetter
    public void setExclusionPatterns(@Nullable String exclusionPatterns) {
        this.exclusionPatterns = exclusionPatterns;
    }

    @Override
    public OnboardApplicationFastScanBuilder.OnboardApplicationFastScanDescriptorImpl getDescriptor() {
        return (OnboardApplicationFastScanBuilder.OnboardApplicationFastScanDescriptorImpl) super.getDescriptor();
    }

    @Symbol("imagingOnboardApplicationFastScan")
    @Extension
    public static final class OnboardApplicationFastScanDescriptorImpl extends BaseActionBuilderDescriptor {
        @Inject
        private AipConsoleGlobalConfiguration configuration;

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return OnbordingApplicationBuilder_DescriptorImpl_displayName();
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
