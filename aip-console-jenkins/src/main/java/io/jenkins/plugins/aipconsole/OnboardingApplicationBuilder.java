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
import com.castsoftware.aip.console.tools.core.utils.FileUtils;
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
import java.util.function.Consumer;
import java.util.function.Function;

import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_jobFailure;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_success_analysisComplete;
import static io.jenkins.plugins.aipconsole.Messages.GenericError_error_missingRequiredParameters;
import static io.jenkins.plugins.aipconsole.Messages.JobsSteps_changed;
import static io.jenkins.plugins.aipconsole.Messages.JobsSteps_jobServiceException;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_FastScanRequired;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_displayName;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_feature_notCompatible;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_actionAboutToStart;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_actionCompleted;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_actionDone;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_applicationLookup;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_deliveryConfiguration_done;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_mode;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_runAnalysis_cancelled;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_runAnalysis_disabled;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_scanMode;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_upload;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_upload_done;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_upload_failed;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_missingFilePath;

public class OnboardingApplicationBuilder extends CommonActionBuilder {
    private String applicationGuid;
    private String exclusionPatterns = "";

    @Override
    protected String checkJobParameters() {
        if (StringUtils.isAnyBlank(getApplicationName(), getFilePath())) {
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
                    getPollingCallback(log), Function.identity());
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
    public OnboardingApplicationBuilder(String applicationName, String filePath) {
        setApplicationName(applicationName);
        setFilePath(filePath);
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
        String expandedFilePath = environmentVariables.expand(getFilePath());
        String expandedDomainName = environmentVariables.expand(getDomainName());
        boolean runAnalysis = false;

        if (!runAnalysis && (StringUtils.isEmpty(expandedFilePath) || !FileUtils.exists(expandedFilePath))) {
            logger.println(OnbordingApplicationBuilder_DescriptorImpl_missingFilePath());
            run.setResult(getDefaultResult());
            return;
        }

        boolean OnBoardingModeWasOn = false; //status before processing
        boolean firstScan = true;
        try {
            OnBoardingModeWasOn = applicationService.isOnboardingSettingsEnabled();
            if (!OnBoardingModeWasOn) {
                logger.println(OnbordingApplicationBuilder_DescriptorImpl_label_mode("OFF"));
                run.setResult(getDefaultResult());
                return;
            }

            logger.println(OnbordingApplicationBuilder_DescriptorImpl_label_applicationLookup(expandedAppName));
            boolean onboardApplication = false;
            String existingAppGuid = null;
            ApplicationDto app = applicationService.getApplicationFromName(expandedAppName);
            if (app != null) {
                existingAppGuid = app.getGuid();
                app = applicationService.getApplicationDetails(existingAppGuid);
                firstScan = app.getVersion() == null || StringUtils.isAnyEmpty(app.getImagingTenant(), app.getVersion().getGuid())
                        || !app.isOnboarded();
            } else {
                onboardApplication = true;
            }

            if (firstScan && runAnalysis) {
                logger.println(OnbordingApplicationBuilder_DescriptorImpl_FastScanRequired());
                run.setResult(getDefaultResult());
                return;
            }

            String scanMode = firstScan ? " Fast-scan/Refresh" : " Rescan";
            logger.println(OnbordingApplicationBuilder_DescriptorImpl_label_scanMode(expandedAppName + scanMode));
            //on-boarding
            ApplicationOnboardingDto applicationOnboardingDto;
            String caipVersion = app == null ? null : app.getCaipVersion();
            String targetNode = app == null ? null : app.getTargetNode();
            String sourcePath = "";
            boolean verbose = getDescriptor().configuration.isVerbose();
            JnksLogPollingProviderImpl jnksLogPollingProvider = new JnksLogPollingProviderImpl(run, listener, verbose);

            String uploadAction = StringUtils.isEmpty(existingAppGuid) ? "onboard sources" : "refresh sources content";
            if (!runAnalysis) {
                logger.println(OnbordingApplicationBuilder_DescriptorImpl_label_upload(uploadAction, expandedAppName));
                sourcePath = uploadService.uploadFileForOnboarding(Paths.get(expandedFilePath).toFile(), existingAppGuid);
                logger.println(OnbordingApplicationBuilder_DescriptorImpl_label_upload_done(uploadAction, sourcePath));
            }

            if (firstScan) {
                applicationGuid = existingAppGuid;
                if (onboardApplication) {
                    applicationGuid = applicationService.onboardApplication(expandedAppName, expandedDomainName, verbose, sourcePath);
                    //log.info("Onboard Application job has started: application GUID= " + applicationGuid);
                }

                //Refresh application information
                app = applicationService.getApplicationFromName(expandedAppName);
                caipVersion = app.getCaipVersion();
                targetNode = app.getTargetNode();

                String discoveredAppGuid = applicationService.discoverApplication(applicationGuid, sourcePath,
                        StringUtils.isNotEmpty(applicationGuid) ? "" : "My version", caipVersion, targetNode, verbose, jnksLogPollingProvider);
                logger.println(OnbordingApplicationBuilder_DescriptorImpl_label_actionCompleted(uploadAction, discoveredAppGuid));

                applicationOnboardingDto = applicationService.getApplicationOnboarding(applicationGuid);
                caipVersion = applicationOnboardingDto.getCaipVersion();
                targetNode = applicationOnboardingDto.getTargetNode();
                existingAppGuid = applicationGuid;
            } else if (!runAnalysis) {
                //For RESCAN PROCESS: re-discover
                sourcePath = app.getVersion().getSourcePath();
                Exclusions exclusions = Exclusions.builder().excludePatterns(exclusionPatterns).build();

                //discover-packages
                logger.println(OnbordingApplicationBuilder_DescriptorImpl_label_actionAboutToStart("Delivery Configuration"));
                final DeliveryConfigurationDto[] deliveryConfig = new DeliveryConfigurationDto[1];
                String deliveryConfigurationGuid = applicationService.discoverPackagesAndCreateDeliveryConfiguration(existingAppGuid, sourcePath, exclusions,
                        VersionStatus.IMAGING_PROCESSED, true, (config) -> deliveryConfig[0] = config);
                DeliveryConfigurationDto deliveryConfiguration = deliveryConfig[0];
                logger.println(OnbordingApplicationBuilder_DescriptorImpl_label_deliveryConfiguration_done(deliveryConfigurationGuid));

                //rediscover-application
                logger.println(OnbordingApplicationBuilder_DescriptorImpl_label_actionAboutToStart("Rediscover Application"));
                applicationService.reDiscoverApplication(existingAppGuid, sourcePath, "", deliveryConfiguration,
                        caipVersion, targetNode, verbose, jnksLogPollingProvider);
                logger.println(OnbordingApplicationBuilder_DescriptorImpl_label_actionDone("Rediscover"));
            }

            //Run Analysis or Deep analysis
            if (!runAnalysis || !applicationService.isImagingAvailable()) {
                String message = !runAnalysis ? OnbordingApplicationBuilder_DescriptorImpl_label_runAnalysis_cancelled()
                        : OnbordingApplicationBuilder_DescriptorImpl_label_runAnalysis_disabled();
                logger.println(message);
            } else {
                if (firstScan) {
                    applicationService.runFirstScanApplication(existingAppGuid, targetNode, caipVersion, verbose, jnksLogPollingProvider);
                } else {
                    applicationService.runReScanApplication(existingAppGuid, targetNode, caipVersion, verbose, jnksLogPollingProvider);
                }
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
        return VersionInformation.fromVersionString("2.5.0");
    }

    public String getExclusionPatterns() {
        return exclusionPatterns;
    }

    @DataBoundSetter
    public void setExclusionPatterns(@Nullable String exclusionPatterns) {
        this.exclusionPatterns = exclusionPatterns;
    }

    @Override
    public OnboardingApplicationBuilder.OnboardApplicationDescriptorImpl getDescriptor() {
        return (OnboardingApplicationBuilder.OnboardApplicationDescriptorImpl) super.getDescriptor();
    }

    @Symbol("imagingOnboardApplication")
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
