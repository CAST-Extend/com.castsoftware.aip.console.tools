package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.jobs.FileCommandRequest;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobRequestBuilder;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobStatusWithSteps;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobType;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApiKeyMissingException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.UploadException;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.ChunkedUploadService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.core.utils.FilenameUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Deliver an application version on AIP Console
 */
@Component
@CommandLine.Command(
        name = "Deliver",
        mixinStandardHelpOptions = true,
        aliases = {"deliver"},
        description = "Delivers a new version to AIP Console"
)
@Slf4j
@Getter
@Setter
public class DeliverVersionCommand implements Callable<Integer> {
    private final RestApiService restApiService;
    private final JobsService jobsService;
    private final ChunkedUploadService uploadService;
    private final ApplicationService applicationService;

    @CommandLine.Mixin
    private SharedOptions sharedOptions;

    @CommandLine.Option(names = {"-n", "--app-name"}, paramLabel = "APPLICATION_NAME", description = "The Name of the application to rescan")
    private String applicationName;
    @CommandLine.Option(names = {"-f", "--file"}, paramLabel = "FILE", description = "A local zip or tar.gz file OR a path to a folder on the node where the source if saved", required = true)
    private File filePath;
    @CommandLine.Option(names = {"-v", "--version-name"}, paramLabel = "VERSION_NAME", description = "The name of the version to create")
    private String versionName;
    @CommandLine.Option(names = {"-d", "--auto-deploy"}, description = "Deploys the version after the delivery (marks the version as current)")
    private boolean autoDeploy;
    @CommandLine.Option(names = {"-c", "--clone", "--rescan"}, description = "Clones the latest version configuration instead of creating a new application")
    private boolean cloneVersion = false;
    @CommandLine.Option(names = "--auto-create", description = "If the given application name doesn't exist on the target server, it'll be automatically created before creating a new version")
    private boolean autoCreate = false;
    @CommandLine.Option(names = "--enable-security-dataflow", description = "If defined, this will activate the security dataflow for this version")
    private boolean enableSecurityDataflow = false;
    @CommandLine.Option(names = "--node-name", paramLabel = "NODE_NAME", description = "The name of the node on which the application will be created. Ignored if no --auto-create or the application already exists.")
    private String nodeName;
    @CommandLine.Option(names = {"-b", "--backup"}, description = "Enable backup of application before delivering the new version")
    private boolean backupEnabled = false;
    @CommandLine.Option(names = "--backup-name", paramLabel = "BACKUP_NAME", description = "The name of the backup to create before delivering the new version. Defaults to 'backup_date.time'")
    private String backupName;

    public DeliverVersionCommand(RestApiService restApiService, JobsService jobsService, ChunkedUploadService chunkedUploadService, ApplicationService applicationService) {
        this.restApiService = restApiService;
        this.jobsService = jobsService;
        this.uploadService = chunkedUploadService;
        this.applicationService = applicationService;
    }

    @Override
    public Integer call() throws Exception {
        // Same as a part of the AddVersion command
        // Upload a local file or register a remote path
        // And then starts a job up to "Delivery"
        // Should deploy/mark as current
        if (StringUtils.isBlank(applicationName)) {
            log.error("No application name provided. Exiting.");
            return Constants.RETURN_APPLICATION_INFO_MISSING;
        }

        try {
            if (sharedOptions.getTimeout() != Constants.DEFAULT_HTTP_TIMEOUT) {
                restApiService.setTimeout(sharedOptions.getTimeout(), TimeUnit.SECONDS);
            }
            restApiService.validateUrlAndKey(sharedOptions.getFullServerRootUrl(), sharedOptions.getUsername(), sharedOptions.getApiKeyValue());
        } catch (ApiKeyMissingException e) {
            return Constants.RETURN_NO_PASSWORD;
        } catch (ApiCallException e) {
            return Constants.RETURN_LOGIN_ERROR;
        }
        String applicationGuid;

        try {
            log.info("Searching for application '{}' on AIP Console", applicationName);
            applicationGuid = applicationService.getOrCreateApplicationFromName(applicationName, autoCreate, nodeName);
            if (StringUtils.isBlank(applicationGuid)) {
                String message = autoCreate ?
                        "Creation of the application '{}' failed on AIP Console" :
                        "Application '{}' was not found on AIP Console";
                log.error(message, applicationName);
                return Constants.RETURN_APPLICATION_NOT_FOUND;
            }

            // AIP Console source path, starting with either "sources:" or "upload:" if it is a folder on the server
            // or a file that was uploaded
            String sourcePath;
            // means it is a subfolder inside the source.folder.location defined in Console
            if (!StringUtils.equalsAnyIgnoreCase(FilenameUtils.getFileExtension(filePath.getName()), Constants.ALLOWED_ARCHIVE_EXTENSIONS)) {
                //call api to check if the folder exists
                try {
                    FileCommandRequest fileCommandRequest = FileCommandRequest.builder().command("LS").path("SOURCES:" + filePath.toPath().toString()).build();
                    restApiService.postForEntity("/api/applications/" + applicationGuid + "/server-folders", fileCommandRequest, String.class);
                    sourcePath = "sources:" + filePath.toPath().toString();
                } catch (ApiCallException e) {
                    return Constants.RETURN_SOURCE_FOLDER_NOT_FOUND;
                }
            } else {
                sourcePath = UUID.randomUUID().toString() + "." + FilenameUtils.getFileExtension(filePath.getName());
                try (InputStream stream = Files.newInputStream(filePath.toPath())) {
                    long fileSize = filePath.length();
                    if (!uploadService.uploadInputStream(applicationGuid, sourcePath, fileSize, stream)) {
                        log.error("Local file fully uploaded, but AIP Console expects more content (fileSize on AIP Console not reached). Check the file you provided wasn't modified since the start of the CLI");
                        return Constants.RETURN_UPLOAD_ERROR;
                    }
                    sourcePath = "upload:" + applicationName + "/main_sources";
                } catch (IOException e) {
                    log.error("Unable to read archive content to be uploaded.", e);
                    throw new UploadException(e);
                }

            }
            // check that the application actually has versions, otherwise it's just an add version job
            cloneVersion = cloneVersion && applicationService.applicationHasVersion(applicationGuid);

            JobRequestBuilder builder = JobRequestBuilder.newInstance(applicationGuid, sourcePath, cloneVersion ? JobType.CLONE_VERSION : JobType.ADD_VERSION)
                    .endStep(autoDeploy ? Constants.ACCEPTANCE_STEP_NAME : Constants.DELIVER_VERSION)
                    .versionName(versionName)
                    .releaseAndSnapshotDate(new Date())
                    .securityObjective(enableSecurityDataflow)
                    .backupApplication(backupEnabled)
                    .backupName(backupName);

            String jobGuid = jobsService.startAddVersionJob(builder);
            JobStatusWithSteps jobStatus = jobsService.pollAndWaitForJobFinished(jobGuid, Function.identity());
            if (JobState.COMPLETED == jobStatus.getState()) {
                log.info("Delivery of application {} was completed successfully.", applicationName);
                return Constants.RETURN_OK;
            }

            log.error("Job did not complete. Status is '{}' on step '{}'", jobStatus.getState(), jobStatus.getFailureStep());
            return Constants.RETURN_JOB_FAILED;
        } catch (ApplicationServiceException e) {
            return Constants.RETURN_APPLICATION_INFO_MISSING;
        } catch (UploadException e) {
            log.error("Error occurred while attempting to upload the given file.", e);
            return Constants.RETURN_UPLOAD_ERROR;
        } /*catch (JobServiceException e) {
            return Constants.RETURN_JOB_POLL_ERROR;
        }*/
    }
}
