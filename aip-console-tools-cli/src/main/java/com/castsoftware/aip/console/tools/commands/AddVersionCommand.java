package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobRequestBuilder;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobStatusWithSteps;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobType;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApiKeyMissingException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.PackagePathInvalidException;
import com.castsoftware.aip.console.tools.core.exceptions.UploadException;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.services.UploadService;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
@CommandLine.Command(
        name = "AddVersion",
        mixinStandardHelpOptions = true,
        aliases = {"add"},
        description = "Creates a new version, runs an analysis and creates a snapshot for an application on AIP Console"
)
@Slf4j
@Getter
@Setter
public class AddVersionCommand implements Callable<Integer> {
    private final RestApiService restApiService;
    private final JobsService jobsService;
    private final UploadService uploadService;
    private final ApplicationService applicationService;

    @CommandLine.Mixin
    private SharedOptions sharedOptions;

    public AddVersionCommand(RestApiService restApiService, JobsService jobsService, UploadService uploadService, ApplicationService applicationService) {
        this.restApiService = restApiService;
        this.jobsService = jobsService;
        this.uploadService = uploadService;
        this.applicationService = applicationService;
    }

    /**
     * The application name to look for on AIP Console
     */
    @CommandLine.Option(names = {"-n", "--app-name"}, paramLabel = "APPLICATION_NAME", description = "The Name of the application to rescan")
    private String applicationName;
    /**
     * The application GUID  on AIP Console
     */
    @CommandLine.Option(names = {"-a", "--app-guid"}, paramLabel = "APPLICATION_GUID", description = "The GUID of the application to rescan")
    private String applicationGuid;
    /**
     * A File that will be uploaded to AIP Console for the given application
     */
    @CommandLine.Option(names = {"-f", "--file"}, paramLabel = "FILE", description = "A local zip or tar.gz file OR a path to a folder on the node where the source if saved", required = true)
    private File filePath;
    /**
     * The Name fo the version from the command line
     */
    @CommandLine.Option(names = {"-v", "--version-name"}, paramLabel = "VERSION_NAME", description = "The name of the version to create")
    private String versionName;

    @CommandLine.Option(names = "--snapshot-name", paramLabel = "SNAPSHOT_NAME", description = "The name of the snapshot to generate")
    private String snapshotName;
    /**
     * Disable cloning previous version automatically.
     */
    @CommandLine.Option(names = {"--no-clone", "--no-rescan", "--new-configuration"},
            description = "Enable this flag to create a new version without cloning the latest version configuration.",
            defaultValue = "false")
    private boolean disableClone = false;
    /**
     * Whether or not to automatically create the application before Adding a version (if the application could not be found)
     */
    @CommandLine.Option(names = "--auto-create", description = "If the given application name doesn't exist on the target server, it'll be automatically created before creating a new version")
    private boolean autoCreate = false;

    @CommandLine.Option(names = "--inplace-mode",
            description = "If true then no history will be kept for delivered sources. In this case, only sources from a unique folder location will be processed.")
    private boolean inPlaceMode = false;

    @CommandLine.Option(names = "--enable-security-dataflow", description = "If defined, this will activate the security dataflow for this version")
    private boolean enableSecurityDataflow = false;

    @CommandLine.Option(names = "--disable-imaging", description = "If provided, uploading data to Imaging will be disabled. Note: If Imaging is not set up, data will not be pushed.")
    private boolean disableImaging = false;

    /**
     * The name of the target node where application will be created. Only used if --auto-create is true and the application doesn't exists
     */
    @CommandLine.Option(names = "--node-name", paramLabel = "NODE_NAME", description = "The name of the node on which the application will be created. Ignored if no --auto-create or the application already exists.")
    private String nodeName;

    /**
     * Run a backup before delivering the new version
     */
    @CommandLine.Option(names = {"-b", "--backup"}, description = "Enable backup of application before delivering the new version")
    private boolean backupEnabled = false;

    /**
     * Name of the backup
     */
    @CommandLine.Option(names = "--backup-name", paramLabel = "BACKUP_NAME", description = "The name of the backup to create before delivering the new version. Defaults to 'backup_date.time'")
    private String backupName;

    /**
     * Domain name
     */
    @CommandLine.Option(names = "--domain-name", paramLabel = "DOMAIN_NAME", description = "The name of the domain to assign to the application. Will be created if it doesn't exists. No domain will be assigned if left empty.")
    private String domainName;

    @CommandLine.Unmatched
    private List<String> unmatchedOptions;

    @Override
    public Integer call() {
        ApiInfoDto apiInfo = null;
        try {
            if (sharedOptions.getTimeout() != Constants.DEFAULT_HTTP_TIMEOUT) {
                restApiService.setTimeout(sharedOptions.getTimeout(), TimeUnit.SECONDS);
            }
            restApiService.validateUrlAndKey(sharedOptions.getFullServerRootUrl(), sharedOptions.getUsername(), sharedOptions.getApiKeyValue());
            apiInfo = restApiService.getAipConsoleApiInfo();
        } catch (ApiKeyMissingException e) {
            return Constants.RETURN_NO_PASSWORD;
        } catch (ApiCallException e) {
            return Constants.RETURN_LOGIN_ERROR;
        }

        log.info("AddVersion version command has triggered with log output = '{}'", sharedOptions.isVerbose());

        if (StringUtils.isBlank(applicationName) && StringUtils.isBlank(applicationGuid)) {
            log.error("No application name or application guid provided. Exiting.");
            return Constants.RETURN_APPLICATION_INFO_MISSING;
        }

        try {
            if (StringUtils.isBlank(applicationGuid)) {
                log.info("Searching for application '{}' on AIP Console", applicationName);
                applicationGuid = applicationService.getOrCreateApplicationFromName(applicationName, autoCreate, nodeName, domainName, inPlaceMode);
                applicationGuid = applicationService.getOrCreateApplicationFromName(applicationName, autoCreate, nodeName, domainName, sharedOptions.isVerbose());
                if (StringUtils.isBlank(applicationGuid)) {
                    String message = autoCreate ?
                            "Creation of the application '{}' failed on AIP Console" :
                            "Application '{}' was not found on AIP Console";
                    log.error(message, applicationName);
                    return Constants.RETURN_APPLICATION_NOT_FOUND;
                }
            }

            if (StringUtils.isEmpty(applicationName) && StringUtils.isNotEmpty(applicationGuid)) {
                applicationName = applicationService.getApplicationNameFromGuid(applicationGuid);
            }

            ApplicationDto app = applicationService.getApplicationFromName(applicationName);
            if (app.isInPlaceMode() && Files.isRegularFile(filePath.toPath())) {
                log.error("The application is created in \"in-place\" mode, only folder path is allowed to deliver in this mode.");
                return Constants.RETURN_INPLACE_MODE_ERROR;
            }

            String sourcePath = uploadService.uploadFileAndGetSourcePath(applicationName, applicationGuid, filePath);

            // check that the application actually has versions, otherwise it's just an add version job
            boolean cloneVersion = (app.isInPlaceMode() || !disableClone) && applicationService.applicationHasVersion(applicationGuid);

            JobRequestBuilder builder = JobRequestBuilder.newInstance(applicationGuid, sourcePath, cloneVersion ? JobType.CLONE_VERSION : JobType.ADD_VERSION)
                    .versionName(versionName)
                    .releaseAndSnapshotDate(new Date())
                    .securityObjective(enableSecurityDataflow)
                    .backupApplication(backupEnabled)
                    .backupName(backupName);

            if (apiInfo.isImagingFlat() && !disableImaging) {
                // Add the imaging step if it is configured
                builder.endStep(Constants.PROCESS_IMAGING);
            }

            String deliveryConfigGuid = applicationService.createDeliveryConfiguration(applicationGuid, sourcePath, null);
            if (StringUtils.isNotBlank(deliveryConfigGuid)) {
                builder.deliveryConfigGuid(deliveryConfigGuid);
            }

            if (StringUtils.isNotBlank(snapshotName)) {
                builder.snapshotName(snapshotName);
            }

            String jobGuid = jobsService.startAddVersionJob(builder);
            // add a shutdown hook, to cancel the job
            Thread shutdownHook = getShutdownHookForJobGuid(jobGuid);
            // Register shutdown hook to cancel the job
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            JobStatusWithSteps jobStatus = jobsService.pollAndWaitForJobFinished(jobGuid, Function.identity(), sharedOptions.isVerbose());
            // Deregister the shutdown hook since the job is finished and we won't need to cancel it
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            if (JobState.COMPLETED == jobStatus.getState()) {
                log.info("Job completed successfully.");
                return Constants.RETURN_OK;
            }


            log.error("Job did not complete. Status is '{}' on step '{}'", jobStatus.getState(), jobStatus.getFailureStep());
            return Constants.RETURN_JOB_FAILED;

        } catch (ApplicationServiceException e) {
            return Constants.RETURN_APPLICATION_INFO_MISSING;
        } catch (UploadException e) {
            log.error("Error occurred while attempting to upload the given file.", e);
            return Constants.RETURN_UPLOAD_ERROR;
        } catch (JobServiceException e) {
            return Constants.RETURN_JOB_POLL_ERROR;
        } catch (PackagePathInvalidException e) {
            log.error(e.getMessage());
            return Constants.RETURN_JOB_FAILED;
        }
    }

    private Thread getShutdownHookForJobGuid(String jobGuid) {
        return new Thread(() -> {
            log.info("Received termination signal. Cancelling currently running job on AIP Console and exiting.");
            try {
                jobsService.cancelJob(jobGuid);
            } catch (JobServiceException e) {
                log.error("Cannot cancel the job on AIP Console. Please cancel it manually.", e);
            }
        });
    }
}
