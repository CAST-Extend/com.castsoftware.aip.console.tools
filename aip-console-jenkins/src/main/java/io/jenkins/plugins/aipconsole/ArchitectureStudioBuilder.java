package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.architecturestudio.ArchitectureModelDto;
import com.castsoftware.aip.console.tools.core.dto.architecturestudio.ArchitectureModelLinkDto;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.utils.VersionInformation;
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
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Set;

import static io.jenkins.plugins.aipconsole.Messages.ArchitectureStudioBuilder_DescriptorImpl_displayName;
import static io.jenkins.plugins.aipconsole.Messages.ArchitectureStudioBuilder_DescriptorImpl_feature_incompatible;
import static io.jenkins.plugins.aipconsole.Messages.ArchitectureStudioBuilder_DescriptorImpl_success;
import static io.jenkins.plugins.aipconsole.Messages.ArchitectureStudioBuilder_ModelChecker_download;
import static io.jenkins.plugins.aipconsole.Messages.ArchitectureStudioBuilder_ModelChecker_error_application;
import static io.jenkins.plugins.aipconsole.Messages.ArchitectureStudioBuilder_ModelChecker_error_availableModels;
import static io.jenkins.plugins.aipconsole.Messages.ArchitectureStudioBuilder_ModelChecker_error_availableModels_modelName;
import static io.jenkins.plugins.aipconsole.Messages.ArchitectureStudioBuilder_ModelChecker_error_modelName;
import static io.jenkins.plugins.aipconsole.Messages.ArchitectureStudioBuilder_ModelChecker_info_application;
import static io.jenkins.plugins.aipconsole.Messages.ArchitectureStudioBuilder_ModelChecker_info_availableModels;
import static io.jenkins.plugins.aipconsole.Messages.ArchitectureStudioBuilder_ModelChecker_info_availableModels_modelName;
import static io.jenkins.plugins.aipconsole.Messages.ArchitectureStudioBuilder_ModelChecker_info_models;
import static io.jenkins.plugins.aipconsole.Messages.ArchitectureStudioBuilder_ModelChecker_success;
import static io.jenkins.plugins.aipconsole.Messages.GenericError_error_missingRequiredParameters;

public class ArchitectureStudioBuilder extends  CommonActionBuilder {

    @CheckForNull
    private String applicationName;
    @CheckForNull
    private String modelName;

    @Nullable
    private String reportPath;
    private long sleepDuration;

    @Override
    protected String checkJobParameters() {
        if (StringUtils.isBlank(getApplicationName())) {
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

    @DataBoundConstructor
    public ArchitectureStudioBuilder(String applicationName) {
        setApplicationName(applicationName);
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws
            InterruptedException, IOException {
        super.perform(run, filePath, launcher, listener);

        //check for min version
        String apiVersion = applicationService.getAipConsoleApiInfo().getApiVersion();
        if (getMinVersion() != null && StringUtils.isNotEmpty(apiVersion)) {
            VersionInformation serverApiVersion = VersionInformation.fromVersionString(apiVersion);
            if (serverApiVersion != null && getMinVersion().isHigherThan(serverApiVersion)) {
                listener.error(ArchitectureStudioBuilder_DescriptorImpl_feature_incompatible("Model Checker", apiVersion, getMinVersion().toString()));
                run.setResult(Result.FAILURE);
                return;
            }
        }

        EnvVars vars = run.getEnvironment(listener);
        String expandedAppName = vars.expand(getApplicationName());
        String expandedModelName = vars.expand(getModelName());
        String expandedReportPath = vars.expand(getReportPath());

        if (StringUtils.isBlank(expandedModelName)) {
            listener.error(ArchitectureStudioBuilder_ModelChecker_error_modelName());
            run.setResult(Result.FAILURE);
            return;
        }
        logger.println(ArchitectureStudioBuilder_ModelChecker_info_models());
        Set<ArchitectureModelDto> modelDtoSet;
        try {
            modelDtoSet = architectureStudioService.getArchitectureModels();
        } catch (ApplicationServiceException e) {
            throw new RuntimeException(e);
        }

        if(!modelDtoSet.isEmpty()){
            logger.println(ArchitectureStudioBuilder_ModelChecker_info_availableModels());
            int index = 1;
            for (ArchitectureModelDto dto : modelDtoSet) {
                logger.println(ArchitectureStudioBuilder_ModelChecker_info_availableModels_modelName(index, dto.getName()));
                index++;
            }
        } else {
            listener.error(ArchitectureStudioBuilder_ModelChecker_error_availableModels());
            run.setResult(Result.FAILURE);
            return;
        }

        /* Search name of the model in the list of available models and get the model details. */
        ArchitectureModelDto modelInUse = modelDtoSet
                .stream()
                .filter(m -> m.getName().equalsIgnoreCase(expandedModelName))
                .findFirst()
                .orElse(null);

        //Check if model list is empty
        if (modelInUse == null){
            listener.error(ArchitectureStudioBuilder_ModelChecker_error_availableModels_modelName(expandedModelName));
            run.setResult(Result.FAILURE);
            return;
        }

        String path = modelInUse.getPath();

        ApplicationDto app;
        try {
            app = applicationService.getApplicationFromName(expandedAppName);
        } catch (ApplicationServiceException e) {
            throw new RuntimeException(e);
        }
        if (app == null){
            listener.error(ArchitectureStudioBuilder_ModelChecker_error_application(expandedAppName));
            run.setResult(Result.FAILURE);
            return;
        }
        logger.println(ArchitectureStudioBuilder_ModelChecker_info_application(expandedAppName));

        logger.println(ArchitectureStudioBuilder_ModelChecker_success(expandedAppName));

        Set<ArchitectureModelLinkDto> modelChecker;
        try {
            modelChecker = architectureStudioService.modelChecker(app.getGuid(), path, app.getCaipVersion());
        } catch (ApiCallException e) {
            throw new RuntimeException(e);
        }

        logger.println(ArchitectureStudioBuilder_ModelChecker_download());

        //Check the transaction Id part
        Integer transactionId = null;
        try {
            architectureStudioService.downloadCheckedModelReport(app.getGuid(), modelInUse.getName(), modelInUse.getMetricId(), modelInUse.getDescription(), transactionId, modelChecker, expandedReportPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        logger.println(ArchitectureStudioBuilder_DescriptorImpl_success());
        run.setResult(Result.SUCCESS);
    }

    private static VersionInformation getMinVersion() {
        //This version can be null if failed to convert from string
        return VersionInformation.fromVersionString("2.8.0");
    }

    public String getModelName() { return  modelName; }

    @DataBoundSetter
    public void setModelName(@CheckForNull String modelName) { this.modelName = modelName; }

    public String getReportPath() { return reportPath; }

    @DataBoundSetter
    public void setReportPath(@Nullable String reportPath) { this.reportPath = reportPath; }

    @Override
    public ArchitectureStudioBuilder.ArchitectureStudioDescriptorImpl getDescriptor() {
        return (ArchitectureStudioBuilder.ArchitectureStudioDescriptorImpl) super.getDescriptor();
    }

    @Symbol("ArchitectureStudioModelChecker")
    @Extension
    public static final class ArchitectureStudioDescriptorImpl extends BaseActionBuilderDescriptor {
        @Inject
        private AipConsoleGlobalConfiguration configuration;

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return ArchitectureStudioBuilder_DescriptorImpl_displayName();
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
