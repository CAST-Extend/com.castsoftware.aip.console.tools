package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.ApplicationCommonDetailsDto;
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
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.core.utils.LogUtils;
import com.castsoftware.aip.console.tools.core.utils.VersionInformation;
import com.google.inject.Guice;
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
import javax.inject.Inject;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.jenkins.plugins.aipconsole.Messages.FastScanApplicationBuilder_DescriptorImpl_displayName;
import static io.jenkins.plugins.aipconsole.Messages.FastScanApplicationBuilder_DescriptorImpl_label_actionAboutToStart;
import static io.jenkins.plugins.aipconsole.Messages.FastScanApplicationBuilder_DescriptorImpl_label_actionDone;
import static io.jenkins.plugins.aipconsole.Messages.FastScanApplicationBuilder_DescriptorImpl_label_applicationLookup;
import static io.jenkins.plugins.aipconsole.Messages.FastScanApplicationBuilder_DescriptorImpl_label_deliveryConfiguration;
import static io.jenkins.plugins.aipconsole.Messages.FastScanApplicationBuilder_DescriptorImpl_label_deliveryConfiguration_done;
import static io.jenkins.plugins.aipconsole.Messages.FastScanApplicationBuilder_DescriptorImpl_label_upload_done;
import static io.jenkins.plugins.aipconsole.Messages.FastScanApplicationBuilder_DescriptorImpl_label_upload_failed;
import static io.jenkins.plugins.aipconsole.Messages.FastScanApplicationBuilder_DescriptorImpl_missingFilePath;
import static io.jenkins.plugins.aipconsole.Messages.FastScanApplicationBuilder_error_jobFailure;
import static io.jenkins.plugins.aipconsole.Messages.FastScanApplicationBuilder_success_analysisComplete;
import static io.jenkins.plugins.aipconsole.Messages.JobsSteps_changed;
import static io.jenkins.plugins.aipconsole.Messages.JobsSteps_jobServiceException;

public class OnboardApplicationFastScanBuilder extends CommonActionBuilder {

    private final static ResourceBundleHolder holder = ResourceBundleHolder.get(io.jenkins.plugins.aipconsole.Messages.class);
    private final static String MESSAGES_STEP_KEY_PREFIX = "FastScanApplicationBuilder.jobSteps.";
    private String applicationGuid;
    private String applicationName;
    private String filePath;
    private String exclusionPatterns = "";
    private long sleepDuration;
    private boolean failureIgnored = false;
    private long timeout = Constants.DEFAULT_HTTP_TIMEOUT;
    @Nullable
    private String nodeName;
    @Nullable
    private String domainName;

    @DataBoundConstructor
    public OnboardApplicationFastScanBuilder(String applicationName, String filePath) {
        this.applicationName = applicationName;
        this.filePath = filePath;
    }

    @Override
    public String getApplicationName() {
        return applicationName;
    }

    @Override
    public String getFilePath() {
        return filePath;
    }

    @Override
    @DataBoundSetter
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public long getSleepDuration() {
        return sleepDuration;
    }

    @DataBoundSetter
    public void setSleepDuration(long sleepDuration) {
        this.sleepDuration = sleepDuration;
    }

    public boolean isFailureIgnored() {
        return failureIgnored;
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
    public FastScanDescriptorImpl getDescriptor() {
        return (FastScanDescriptorImpl) super.getDescriptor();
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
                listener.error(FastScanApplicationBuilder_error_jobFailure());
                run.setResult(Result.FAILURE);
                return null;
            } else {
                log.println(FastScanApplicationBuilder_success_analysisComplete());
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

    @Override
    protected void performClient(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        PrintStream log = listener.getLogger();

        if (apiService == null || jobsService == null) {
            Guice.createInjector(new AipConsoleModule()).injectMembers(this);
        }

        String apiServerUrl = getAipConsoleUrl();
        String apiKey = Secret.toString(getApiKey());

        if (StringUtils.isBlank(apiServerUrl)) {
            run.setResult(Result.NOT_BUILT);
            return;
        }
        if (StringUtils.isBlank(apiKey)) {
            run.setResult(Result.NOT_BUILT);
            return;
        }

        String expandedAppName = environmentVariables.expand(getApplicationName());
        String expandedFilePath = environmentVariables.expand(getFilePath());
        String expandedDomainName = environmentVariables.expand(getDomainName());

        if (StringUtils.isEmpty(expandedFilePath)) {
            log.println(FastScanApplicationBuilder_DescriptorImpl_missingFilePath());
            run.setResult(getDefaultResult());
            return;
        }

        try {
            log.println(FastScanApplicationBuilder_DescriptorImpl_label_applicationLookup(expandedAppName));

            ApplicationCommonDetailsDto app = applicationService.getApplicationDetailsFromName(expandedAppName);
            if(app == null){
                log.println("Application not found, starting new upload");
                applicationGuid = null;
            }
            Path path = Paths.get(expandedFilePath);
            String sourcePath = uploadService.uploadFileForOnboarding(
                    path.toFile(), applicationGuid);

            log.println(FastScanApplicationBuilder_DescriptorImpl_label_upload_done(sourcePath));

            boolean verbose = getDescriptor().configuration.isVerbose();

            if (app == null) {
                applicationGuid = applicationService.onboardApplication(expandedAppName, expandedDomainName, verbose, sourcePath);
            } else {
                applicationGuid = app.getGuid();
            }

            //Refresh application information
            ApplicationOnboardingDto applicationOnboardingDto = applicationService.getApplicationOnboarding(applicationGuid);
            String caipVersion = applicationOnboardingDto.getCaipVersion();
            String targetNode = applicationOnboardingDto.getTargetNode();

            //For RESCAN PROCESS: re-discover
            Exclusions exclusions = Exclusions.builder().excludePatterns(exclusionPatterns).build();

            //discover-packages
            log.println(FastScanApplicationBuilder_DescriptorImpl_label_deliveryConfiguration());
            DeliveryConfigurationDto[] deliveryConfig = new DeliveryConfigurationDto[1];
            String deliveryConfigurationGuid = applicationService.discoverPackagesAndCreateDeliveryConfiguration(applicationGuid, sourcePath, exclusions,
                    VersionStatus.DELIVERED, true, (config) -> deliveryConfig[0] = config, true);
            DeliveryConfigurationDto deliveryConfiguration = deliveryConfig[0];
            log.println(FastScanApplicationBuilder_DescriptorImpl_label_deliveryConfiguration_done(deliveryConfigurationGuid));
            deliveryConfiguration.setGuid(deliveryConfigurationGuid);

            //rediscover-application
            log.println(FastScanApplicationBuilder_DescriptorImpl_label_actionAboutToStart("Fast-Scan"));
            JnksLogPollingProviderImpl jnksLogPollingProvider = new JnksLogPollingProviderImpl(jobsService, run, listener, verbose, sleepDuration);
            String jobStatus = applicationService.fastScan(applicationGuid, sourcePath, "", deliveryConfiguration,
                    caipVersion, targetNode, verbose, jnksLogPollingProvider);
            if(jobStatus != null && jobStatus.equalsIgnoreCase(JobState.COMPLETED.toString())) {
                log.println(FastScanApplicationBuilder_DescriptorImpl_label_actionDone("Fast-Scan"));
            }
        } catch (ApplicationServiceException | JobServiceException e) {
            e.printStackTrace(log);
            run.setResult(getDefaultResult());
            return;
        } catch (UploadException e) {
            listener.error(FastScanApplicationBuilder_DescriptorImpl_label_upload_failed());
            e.printStackTrace(log);
            run.setResult(getDefaultResult());
            return;
        } catch (PackagePathInvalidException e) {
            log.println(JobsSteps_jobServiceException());
            listener.error(JobsSteps_jobServiceException());
            e.printStackTrace(log);
            run.setResult(getDefaultResult());
            return;
        }
    }

    @Symbol("FastScan")
    @Extension
    public static final class FastScanDescriptorImpl extends BaseActionBuilderDescriptor {
        @Inject
        private AipConsoleGlobalConfiguration configuration;

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return FastScanApplicationBuilder_DescriptorImpl_displayName();
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
