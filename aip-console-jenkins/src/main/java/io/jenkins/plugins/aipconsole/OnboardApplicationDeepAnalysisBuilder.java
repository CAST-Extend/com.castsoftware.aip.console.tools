package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.ModuleGenerationType;
import com.castsoftware.aip.console.tools.core.dto.VersionStatus;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.UploadException;
import com.castsoftware.aip.console.tools.core.utils.VersionInformation;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
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
import java.nio.file.Paths;

import static io.jenkins.plugins.aipconsole.Messages.GenericError_error_missingRequiredParameters;
import static io.jenkins.plugins.aipconsole.Messages.OnboardApplicationDeepAnalysisBuilder_DescriptorImpl_displayName;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_DeepAnalysisForbidden;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_FastScanRequired;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_applicationLookup;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_label_mode;

public class OnboardApplicationDeepAnalysisBuilder extends CommonActionBuilder {
    final static boolean runAnalysis = true;
    @Nullable
    private String snapshotName;
    private long sleepDuration;

    private boolean includeFastScan = false;

    private String sourcePath;

    private String moduleGenerationType = ModuleGenerationType.FULL_CONTENT.toString();

    @DataBoundSetter
    public void setModuleGenerationType(String moduleGenerationType) {
        this.moduleGenerationType = moduleGenerationType;
    }

    public String getModuleGenerationType() {
        return moduleGenerationType;
    }

    @Override
    protected String checkJobParameters() {
        if (StringUtils.isBlank(getApplicationName())) {
            return GenericError_error_missingRequiredParameters();
        }
        return super.checkJobParameters();
    }

    public boolean isIncludeFastScan() {
        return includeFastScan;
    }

    @DataBoundSetter
    public void setIncludeFastScan(boolean includeFastScan) {
        this.includeFastScan = includeFastScan;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    @DataBoundSetter
    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public long getSleepDuration() {
        return sleepDuration;
    }

    @DataBoundSetter
    public void setSleepDuration(long sleepDuration) {
        this.sleepDuration = sleepDuration;
    }

    @DataBoundConstructor
    public OnboardApplicationDeepAnalysisBuilder(String applicationName) {
        setApplicationName(applicationName);
    }

    @Override
    protected void performClient(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        String expandedAppName = environmentVariables.expand(getApplicationName());
        EnvVars vars = run.getEnvironment(listener);
        String expandedSourcePath = vars.expand(sourcePath);
        String expandedIncludeFastScan = vars.expand(environmentVariables.get("INCLUDE_FAST_SCAN"));
        try {
            if (!applicationService.isOnboardingSettingsEnabled()) {
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
                firstScan = app == null || app.getVersion() == null || StringUtils.isEmpty(app.getVersion().getGuid()) || !app.isOnboarded();
            }
            String _sourcePath = "";
            if(Boolean.valueOf(expandedIncludeFastScan)){
                _sourcePath = uploadService.uploadFileForOnboarding(Paths.get(expandedSourcePath).toFile(), existingAppGuid);
                if(app.getVersion().getStatus() == VersionStatus.ANALYZED) {
                    logger.println("Fast Scan will be done before running deep analysis");
                } else {
                    logger.println("Application should be analyzed to include fast scan in deep analysis. Deep analysis will continue without fast scan");
                }
            }

            logger.println("Fast scan included: " + expandedIncludeFastScan);

            if (firstScan || app == null || !app.isOnboarded()) {

                logger.println((app != null && !app.isOnboarded())
                        ? OnbordingApplicationBuilder_DescriptorImpl_DeepAnalysisForbidden()
                        : OnbordingApplicationBuilder_DescriptorImpl_FastScanRequired());
                run.setResult(getDefaultResult());
                return;
            }

            //on-boarding
            String caipVersion = app.getCaipVersion();
            String targetNode = app.getTargetNode();
            boolean verbose = getDescriptor().configuration.isVerbose();
            JenkinsLogPollingProviderServiceImpl jnksLogPollingProvider = new JenkinsLogPollingProviderServiceImpl(jobsService, run, listener, verbose, getSleepDuration());

            //WEBITOOLS-214: Run Analysis or Deep analysis even when no Imaging available
            String expandedSsnapshotName = environmentVariables.expand(getSnapshotName());
            ModuleGenerationType moduleType = ModuleGenerationType.FULL_CONTENT; //default
            if (StringUtils.isNotEmpty(moduleGenerationType)) {
                moduleType = ModuleGenerationType.fromString(moduleGenerationType);
            }

            applicationService.runDeepAnalysis(existingAppGuid, targetNode, caipVersion
                    , expandedSsnapshotName, moduleType, verbose
                    , Boolean.valueOf(expandedIncludeFastScan), _sourcePath, jnksLogPollingProvider);
        } catch (ApplicationServiceException e) {
            e.printStackTrace(logger);
            run.setResult(getDefaultResult());
            return;
        } catch (UploadException e) {
            throw new RuntimeException(e);
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
