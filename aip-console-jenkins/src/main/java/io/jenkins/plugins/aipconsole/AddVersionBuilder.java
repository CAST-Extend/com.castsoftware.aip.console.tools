package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.NodeDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobStatus;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.UploadException;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.ChunkedUploadService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.fasterxml.jackson.core.type.TypeReference;
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
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.Secret;
import io.jenkins.plugins.aipconsole.config.AipConsoleGlobalConfiguration;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_accessDenied;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_appCreateError;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_appNotFound;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_fileNotFound;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_jobFailure;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_jobServiceException;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_missingRequiredParameters;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_noApiKey;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_noServerUrl;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_nodeNotFound;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_uploadFailed;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_info_appNotFoundAutoCreate;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_info_noVersionAvailable;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_info_pollJobMessage;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_info_startAddVersionJob;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_info_startCloneVersionJob;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_info_startUpload;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_success_analysisComplete;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_DescriptorImpl_displayName;
import static io.jenkins.plugins.aipconsole.Messages.CreateApplicationBuilder_CreateApplication_error_jobServiceException;
import static io.jenkins.plugins.aipconsole.Messages.JobsSteps_changed;

public class AddVersionBuilder extends Builder implements SimpleBuildStep {

    public static final int BUFFER_SIZE = 10 * 1024 * 1024;
    @Inject
    private JobsService jobsService;

    @Inject
    private ChunkedUploadService chunkedUploadService;

    @Inject
    private RestApiService apiService;

    @Inject
    private ApplicationService applicationService;

    private String applicationName;
    private String applicationGuid;
    private String filePath;
    private boolean autoCreate = false;
    private boolean cloneVersion = false;
    @Nullable
    private String versionName = "";
    private long timeout = Constants.DEFAULT_HTTP_TIMEOUT;
    private boolean failureIgnored = false;
    @Nullable
    private String nodeName = "";
    private boolean enableSecurityDataflow = false;

    @DataBoundConstructor
    public AddVersionBuilder(String applicationName, String filePath) {
        this.applicationName = applicationName;
        this.filePath = filePath;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationGuid() {
        return applicationGuid;
    }

    @DataBoundSetter
    public void setApplicationGuid(String applicationGuid) {
        this.applicationGuid = applicationGuid;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isAutoCreate() {
        return autoCreate;
    }

    @DataBoundSetter
    public void setAutoCreate(boolean autoCreate) {
        this.autoCreate = autoCreate;
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

    @Nullable
    public String getNodeName() {
        return nodeName;
    }

    @DataBoundSetter
    public void setNodeName(@Nullable String nodeName) {
        this.nodeName = nodeName;
    }

    public boolean isEnableSecurityDataflow() {
        return enableSecurityDataflow;
    }

    @DataBoundSetter
    public void setEnableSecurityDataflow(boolean enableSecurityDataflow) {
        this.enableSecurityDataflow = enableSecurityDataflow;
    }

    @Override
    public AddVersionDescriptorImpl getDescriptor() {
        return (AddVersionDescriptorImpl) super.getDescriptor();
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        PrintStream log = listener.getLogger();
        Result defaultResult = failureIgnored ? Result.UNSTABLE : Result.FAILURE;
        boolean applicationHasVersion = this.cloneVersion;

        String errorMessage;
        if ((errorMessage = checkJobParameters()) != null) {
            listener.error(errorMessage);
            run.setResult(Result.NOT_BUILT);
            return;
        }

        EnvVars vars = run.getEnvironment(listener);
        String resolvedFilePath = vars.expand(filePath);
        FilePath workspaceFile = workspace.child(resolvedFilePath);
        if (!workspaceFile.exists()) {
            listener.error(AddVersionBuilder_AddVersion_error_fileNotFound(filePath));
            run.setResult(Result.NOT_BUILT);
            return;
        }

        // Check the services have been properly initialized

        if (!ObjectUtils.allNotNull(apiService, chunkedUploadService, jobsService, applicationService)) {
            // Manually setup Guice Injector using Module (Didn't find any way to make this automatically)
            Injector injector = Guice.createInjector(new AipConsoleModule());
            // Guice can automatically inject those, but then findbugs, not seeing the change,
            // will fail the build considering they will provoke an NPE
            // So, to avoid this, set them explicitly (if they were not set)
            apiService = injector.getInstance(RestApiService.class);
            chunkedUploadService = injector.getInstance(ChunkedUploadService.class);
            jobsService = injector.getInstance(JobsService.class);
            applicationService = injector.getInstance(ApplicationService.class);
        }

        String apiServerUrl = getDescriptor().getAipConsoleUrl();
        String apiKey = Secret.toString(getDescriptor().getAipConsoleSecret());
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
            listener.error(AddVersionBuilder_AddVersion_error_accessDenied(apiServerUrl));
            run.setResult(defaultResult);
            return;
        }

        String randomizedFileName = UUID.randomUUID().toString();

        try {

            // Get the GUID from AIP Console
            applicationGuid = applicationService.getApplicationGuidFromName(applicationName);

            if (StringUtils.isBlank(applicationGuid)) {
                if (!autoCreate) {
                    listener.error(AddVersionBuilder_AddVersion_error_appNotFound(applicationName));
                    run.setResult(defaultResult);
                    return;
                }
                // Is there a node name
                String nodeGuid = null;
                if (StringUtils.isNotBlank(nodeName)) {
                    try {
                        nodeGuid = apiService.getForEntity("/api/nodes",
                                new TypeReference<List<NodeDto>>() {
                                }).stream()
                                .filter(n -> StringUtils.equalsIgnoreCase(n.getName(), nodeName))
                                .map(NodeDto::getGuid)
                                .findFirst()
                                .orElse(null);

                        if (StringUtils.isBlank(nodeGuid)) {
                            listener.error(AddVersionBuilder_AddVersion_error_nodeNotFound(nodeName));
                            run.setResult(defaultResult);
                            return;
                        }
                    } catch (ApiCallException e) {
                        listener.error("Unable to retrieve the node guid from the given name");
                        e.printStackTrace(log);
                        run.setResult(defaultResult);
                        return;
                    }
                }

                log.println(AddVersionBuilder_AddVersion_info_appNotFoundAutoCreate(applicationName));
                String jobGuid = jobsService.startCreateApplication(applicationName, nodeGuid);
                applicationGuid = jobsService.pollAndWaitForJobFinished(jobGuid,
                        jobStatusWithSteps -> log.println(JobsSteps_changed(JobStepTranslationHelper.getStepTranslation(jobStatusWithSteps.getProgressStep()))),
                        s -> s.getState() == JobState.COMPLETED ? s.getAppGuid() : null);
                if (StringUtils.isBlank(applicationGuid)) {
                    listener.error(CreateApplicationBuilder_CreateApplication_error_jobServiceException(applicationName, apiServerUrl));
                    run.setResult(defaultResult);
                    return;
                }
                // Don't clone version if we just created the application
                applicationHasVersion = false;
            }

            // If user asks for a "rescan" (i.e. clone previous version config)
            // check that there are versions on the application before launching the clone job
            if (applicationHasVersion) {
                applicationHasVersion = applicationService.applicationHasVersion(applicationGuid);
            }

            log.println(AddVersionBuilder_AddVersion_info_startUpload(FilenameUtils.getName(resolvedFilePath)));
            // Rename the file to applicationName-versionName.ext
            if (StringUtils.endsWithIgnoreCase(resolvedFilePath, ".tar.gz")) {
                // getExtension only returns the last extension, so specific case for .tar.gz
                randomizedFileName += ".tar.gz";
            } else {
                randomizedFileName += "." + FilenameUtils.getExtension(resolvedFilePath);
            }

            // if it already exists, delete it (might be a remnant of a previous execution)
            // move source file to another file name, to avoid conflicts when uploading the same zip file for multiple applications
            try (InputStream workspaceFileStream = workspaceFile.read();
                 InputStream bufferedStream = new BufferedInputStream(workspaceFileStream, BUFFER_SIZE)) {
                log.println("Uploading file " + workspaceFile.getName());
                if (!chunkedUploadService.uploadInputStream(applicationGuid, randomizedFileName, workspaceFile.length(), bufferedStream)) {
                    throw new UploadException("Uploading was not completed successfully.");
                }
            }

        } catch (ApplicationServiceException e) {
            listener.error(AddVersionBuilder_AddVersion_error_appCreateError(applicationName));
            e.printStackTrace(listener.getLogger());
            run.setResult(defaultResult);
            return;
        } catch (UploadException e) {
            listener.error(AddVersionBuilder_AddVersion_error_uploadFailed());
            e.printStackTrace(listener.getLogger());
            run.setResult(defaultResult);
            return;
        } catch (JobServiceException e) {
            listener.error(CreateApplicationBuilder_CreateApplication_error_jobServiceException(applicationName, apiServerUrl));
            e.printStackTrace(listener.getLogger());
            run.setResult(defaultResult);
            return;
        }

        try {
            // Create a value for versionName
            String resolvedVersionName = vars.expand(versionName);

            if (StringUtils.isBlank(resolvedVersionName)) {
                DateFormat formatVersionName = new SimpleDateFormat("yyMMdd.HHmmss");
                resolvedVersionName = String.format("v%s", formatVersionName.format(new Date()));
            }

            if (this.cloneVersion) {
                if (applicationHasVersion) {
                    log.println(AddVersionBuilder_AddVersion_info_startCloneVersionJob(applicationName));
                } else {
                    log.println(AddVersionBuilder_AddVersion_info_noVersionAvailable(applicationName));
                }
            } else {
                log.println(AddVersionBuilder_AddVersion_info_startAddVersionJob(applicationName));
            }

            String jobGuid = jobsService.startAddVersionJob(
                    applicationGuid,
                    applicationName,
                    randomizedFileName,
                    resolvedVersionName,
                    new Date(),
                    applicationHasVersion,
                    enableSecurityDataflow
            );

            log.println(AddVersionBuilder_AddVersion_info_pollJobMessage());
            JobState state = pollJob(jobGuid, log);
            if (state != JobState.COMPLETED) {
                listener.error(AddVersionBuilder_AddVersion_error_jobFailure(state.toString()));
                run.setResult(defaultResult);
            } else {
                log.println(AddVersionBuilder_AddVersion_success_analysisComplete());
                run.setResult(Result.SUCCESS);
            }
        } catch (JobServiceException e) {
            listener.error(AddVersionBuilder_AddVersion_error_jobServiceException());
            e.printStackTrace(listener.getLogger());
            run.setResult(defaultResult);
        }
    }

    /**
     * Check some initial elements before running the Job
     *
     * @return The error message based on the issue that was found, null if no issue was found
     */
    private String checkJobParameters() {
        if (StringUtils.isAnyBlank(applicationName, filePath)) {
            return AddVersionBuilder_AddVersion_error_missingRequiredParameters();
        }
        String apiServerUrl = getDescriptor().getAipConsoleUrl();
        String apiKey = Secret.toString(getDescriptor().getAipConsoleSecret());

        if (StringUtils.isBlank(apiServerUrl)) {
            return AddVersionBuilder_AddVersion_error_noServerUrl();
        }
        if (StringUtils.isBlank(apiKey)) {
            return AddVersionBuilder_AddVersion_error_noApiKey();
        }

        return null;
    }

    private JobState pollJob(String jobGuid, PrintStream log) throws JobServiceException {
        return jobsService.pollAndWaitForJobFinished(jobGuid,
                jobStatusWithSteps -> log.println(
                        jobStatusWithSteps.getAppName() + " - " +
                                JobsSteps_changed(JobStepTranslationHelper.getStepTranslation(jobStatusWithSteps.getProgressStep()))
                ),
                JobStatus::getState);
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

        public int getTimeout() {
            return configuration.getTimeout();
        }
    }
}
