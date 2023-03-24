package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.DebugOptionsDto;
import com.castsoftware.aip.console.tools.core.dto.Exclusions;
import com.castsoftware.aip.console.tools.core.dto.ModuleGenerationType;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobExecutionDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobRequestBuilder;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
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
import com.castsoftware.aip.console.tools.core.utils.ApiEndpointHelper;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.core.utils.VersionObjective;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
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

    @CommandLine.Option(names = {"-date", "--version-date"},
            description = "The version date associated with the version to be create: the expected format is \"yyyy-MM-ddTHH:mm:ss\"")
    private String versionDateString;

    @CommandLine.Option(names = "--snapshot-name", paramLabel = "SNAPSHOT_NAME", description = "The name of the snapshot to generate")
    private String snapshotName;
    @CommandLine.Option(names = {"--snapshot-date"},
            description = "The snapshot date associated with the snapshot to be create: the expected format is \"yyyy-MM-ddTHH:mm:ss\"")
    private String snapshotDateString;
    /**
     * Disable cloning previous version automatically.
     */
    @CommandLine.Option(names = {"--no-clone", "--no-rescan", "--new-configuration"},
            description = "Enable this flag to create a new version without cloning the latest version configuration."
                    + " if specified without parameter: ${FALLBACK-VALUE}",
            defaultValue = "false", fallbackValue = "true")
    private boolean disableClone = false;
    /**
     * Whether or not to automatically create the application before Adding a version (if the application could not be found)
     */
    @CommandLine.Option(names = "--auto-create", description = "If the given application name doesn't exist on the target server, it'll be automatically created before creating a new version"
            + " if specified without parameter: ${FALLBACK-VALUE}", fallbackValue = "true")
    private boolean autoCreate = false;

    @CommandLine.Option(names = {"-css", "--css-server"}, description = "CSS Server name that will host the application data: Format will be host:port/databaseName and can be checked on AIP Console's Global Configuration page.")
    private String cssServerName;

    @CommandLine.Option(names = "--enable-security-dataflow", description = "If defined, this will activate the security dataflow for this version"
            + " if specified without parameter: ${FALLBACK-VALUE}",
            fallbackValue = "true")
    private boolean enableSecurityDataflow = false;
    @CommandLine.Option(names = {"--enable-data-safety-investigation", "--enable-data-safety"},
            description = "If defined, this will activate the Data Safety investigation for this version"
                    + " if specified without parameter: ${FALLBACK-VALUE}",
            fallbackValue = "true")
    private boolean enableDataSafety = false;

    @CommandLine.Option(names = "--process-imaging", description = "If provided, will upload data to Imaging"
            + " if specified without parameter: ${FALLBACK-VALUE}", fallbackValue = "true")
    private boolean processImaging = false;

    /**
     * The name of the target node where application will be created. Only used if --auto-create is true and the application doesn't exists
     */
    @CommandLine.Option(names = "--node-name", paramLabel = "NODE_NAME", description = "The name of the node on which the application will be created. Ignored if no --auto-create or the application already exists.")
    private String nodeName;

    /**
     * Run a backup before delivering the new version
     */
    @CommandLine.Option(names = {"-b", "--backup"}, description = "Enable backup of application before delivering the new version"
            + " if specified without parameter: ${FALLBACK-VALUE}", fallbackValue = "true")
    private boolean backupEnabled = false;

    @CommandLine.Option(names = {"--blueprint"}
            , description = "Add blueprint objective to the objectives list (default: ${DEFAULT-VALUE})."
            + " if specified without parameter: ${FALLBACK-VALUE}", fallbackValue = "true", defaultValue = "false")
    private boolean blueprint;

    @CommandLine.Option(names = {"-security-assessment", "--enable-security-assessment"},
            description = "Enable/Disable Security Assessment for this version"
                    + " if specified without parameter: ${FALLBACK-VALUE}",
            fallbackValue = "true", defaultValue = "false")
    private boolean enableSecurityAssessment;

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

    /**
     * Application debug options
     */
    @CommandLine.Option(names = {"-sql", "--show-sql"}
            , description = "Enable or Desible application' Show Sql debug option (default: ${DEFAULT-VALUE})"
            + " if specified without parameter: ${FALLBACK-VALUE}", fallbackValue = "true"
            , defaultValue = "false")
    private boolean showSql;
    @CommandLine.Option(names = {"-amt", "--amt-profiling"}
            , description = "Enable or Desible application' AMT Profiling debug option (default: ${DEFAULT-VALUE})"
            + " if specified without parameter: ${FALLBACK-VALUE}", fallbackValue = "true"
            , defaultValue = "false")
    private boolean amtProfiling;
    @CommandLine.Option(names = {"--consolidation", "--upload-application"},
            description = "When sets to false,  this prevents from consolidating snapshot or from publishing application to the Health dashboard"
                    + " if specified without parameter: ${FALLBACK-VALUE}",
            defaultValue = "true", fallbackValue = "true")
    private boolean consolidation = true;

    @CommandLine.Option(names = "--module-option", description = "Generates a user defined module option forr either technology module or analysis unit module. Possible value is one of: full_content, one_per_au, one_per_techno")
    private ModuleGenerationType moduleGenerationType;

    @CommandLine.Unmatched
    private List<String> unmatchedOptions;

    private JobRequestBuilder builder;

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
        log.info("[Debug options] Show Sql is '{}'", showSql);
        log.info("[Debug options] AMT Profiling is '{}'", amtProfiling);

        if (StringUtils.isBlank(applicationName) && StringUtils.isBlank(applicationGuid)) {
            log.error("No application name or application guid provided. Exiting.");
            return Constants.RETURN_APPLICATION_INFO_MISSING;
        }

        try {
            if (StringUtils.isBlank(applicationGuid)) {
                log.info("Searching for application '{}' on AIP Console", applicationName);
                applicationGuid = applicationService.getOrCreateApplicationFromName(applicationName, autoCreate, nodeName, domainName, cssServerName, sharedOptions.isVerbose());
                if (StringUtils.isBlank(applicationGuid)) {
                    String message = autoCreate ?
                            "Creation of the application '{}' failed on AIP Console" :
                            "Application '{}' was not found on AIP Console";
                    log.error(message, applicationName);
                    return Constants.RETURN_APPLICATION_NOT_FOUND;
                }
            }

            ApplicationDto app = applicationService.getApplicationFromGuid(applicationGuid);
            applicationName = app.getName();
            if (app.isInPlaceMode()) {
                log.info("The application '{}' is using the \"Rapid Delivery Mode\"", applicationName);
            }

            String sourcePath = uploadService.uploadFileAndGetSourcePath(applicationName, applicationGuid, filePath);

            // check that the application actually has versions, otherwise it's just an add version job
            boolean cloneVersion = (app.isInPlaceMode() || !disableClone) && applicationService.applicationHasVersion(applicationGuid);

            builder = JobRequestBuilder.newInstance(applicationGuid, sourcePath, cloneVersion ? JobType.CLONE_VERSION : JobType.ADD_VERSION, app.getCaipVersion())
                    .nodeName(app.getTargetNode())
                    .versionName(versionName)
                    .versionReleaseDate(applicationService.getVersionDate(versionDateString))
                    .snapshotDate(applicationService.getVersionDate(snapshotDateString))
                    .objectives(VersionObjective.DATA_SAFETY, enableDataSafety)
                    .backupApplication(backupEnabled)
                    .backupName(backupName)
                    .processImaging(processImaging);

            String deliveryConfigGuid = applicationService.createDeliveryConfiguration(applicationGuid, sourcePath, Exclusions.builder().build(), cloneVersion);
            if (StringUtils.isNotBlank(deliveryConfigGuid)) {
                builder.deliveryConfigGuid(deliveryConfigGuid);
            }

            builder.objectives(VersionObjective.BLUEPRINT, blueprint);
            builder.objectives(VersionObjective.SECURITY, enableSecurityAssessment);

            applicationService.updateModuleGenerationType(applicationGuid, builder, moduleGenerationType, !cloneVersion);

            if (StringUtils.isNotBlank(snapshotName)) {
                builder.snapshotName(snapshotName);
            }

            //Snapshot required now see whether we upload application or not
            boolean forcedConsolidation = processImaging || consolidation;
            builder.uploadApplication(forcedConsolidation);
            if (!forcedConsolidation) {
                log.info("The snapshot {} for application {} will be taken but will not be published.", snapshotName, applicationName);
                builder.endStep(Constants.SNAPSHOT_INDICATOR);
            } else if (processImaging) {
                builder.endStep(Constants.PROCESS_IMAGING);
            }

            //==============
            // Ony set debug option when ON. Run Analysis always consider these options as OFF(disabled)
            //==============
            DebugOptionsDto oldDebugOptions = applicationService.getDebugOptions(applicationGuid);
            if (showSql) {
                applicationService.updateShowSqlDebugOption(applicationGuid, showSql);
            }
            if (amtProfiling) {
                applicationService.updateAmtProfileDebugOption(applicationGuid, amtProfiling);
            }
            
            log.info("Update JEE and DOTNET security dataflow settings to: {}", enableSecurityDataflow);
            applicationService.updateSecurityDataflow(applicationGuid, enableSecurityDataflow, Constants.JEE_TECHNOLOGY_PATH);
            applicationService.updateSecurityDataflow(applicationGuid, enableSecurityDataflow, Constants.DOTNET_TECHNOLOGY_PATH);

            log.info("Job request : " + builder.buildJobRequest().toString());
            String jobGuid = jobsService.startAddVersionJob(builder);
            // add a shutdown hook, to cancel the job
            Thread shutdownHook = getShutdownHookForJobGuid(jobGuid);
            // Register shutdown hook to cancel the job
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            JobExecutionDto jobStatus = jobsService.pollAndWaitForJobFinished(jobGuid, Function.identity(), sharedOptions.isVerbose());
            // Deregister the shutdown hook since the job is finished and we won't need to cancel it
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            DebugOptionsDto debugOptions = applicationService.getDebugOptions(applicationGuid);
            applicationService.resetDebugOptions(applicationGuid, oldDebugOptions);
            if (JobState.COMPLETED == jobStatus.getState()) {
                if (debugOptions.isActivateAmtMemoryProfile()) {
                    log.info("[Debug options] Amt Profiling file download URL: {}",
                            sharedOptions.getFullServerRootUrl() + ApiEndpointHelper.getAmtProfilingDownloadUrl(applicationGuid));
                }
                log.info("Job completed successfully.");
                return Constants.RETURN_OK;
            }

            log.error("Job did not complete. Status is '{}' on step '{}'", jobStatus.getState(), jobStatus.getCurrentStep());
            return jobStatus.getState() == JobState.CANCELED ? Constants.RETURN_JOB_CANCELED : Constants.RETURN_JOB_FAILED;

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
