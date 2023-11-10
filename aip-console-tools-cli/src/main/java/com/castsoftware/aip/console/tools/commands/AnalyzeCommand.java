package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.DebugOptionsDto;
import com.castsoftware.aip.console.tools.core.dto.ModuleGenerationType;
import com.castsoftware.aip.console.tools.core.dto.VersionDto;
import com.castsoftware.aip.console.tools.core.dto.VersionStatus;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobExecutionDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobRequestBuilder;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobType;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.utils.ApiEndpointHelper;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.core.utils.VersionInformation;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Set;
import java.util.function.Function;

/**
 * Run analysis for an application and a version on AIP Console
 */
@Component
@CommandLine.Command(
        name = "Analysis",
        mixinStandardHelpOptions = true,
        aliases = {"analyze"},
        description = "Analyses an existing version on AIP Console"
)
@Slf4j
@Getter
@Setter
public class AnalyzeCommand extends BasicCallable {
    private static final DateFormat RELEASE_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    @CommandLine.Mixin
    private SharedOptions sharedOptions;

    @CommandLine.Option(names = {"-n", "--app-name"},
            paramLabel = "APPLICATION_NAME",
            description = "The Name of the application to analyze",
            required = true)
    private String applicationName;

    @CommandLine.Option(names = {"-v", "--version-name"},
            paramLabel = "VERSION_NAME",
            description = "The name of the version to analyze. If omitted, the latest version will be used.")
    private String versionName;

    @CommandLine.Option(names = {"-S", "--snapshot"},
            description = "Creates a snapshot after running the analysis." + " if specified without parameter: ${FALLBACK-VALUE}",
            fallbackValue = "true")
    private boolean withSnapshot;

    @CommandLine.Option(names = "--process-imaging", description = "If provided, will upload data to Imaging. Note: Parameter will be ignored if snapshot option is not provided and Imaging is not setup in AIP Console"
            + " if specified without parameter: ${FALLBACK-VALUE}", fallbackValue = "true")
    private boolean processImaging = false;
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

    @CommandLine.Option(names = "--module-option", description = "Generates a user defined module option for either technology module or analysis unit module. Possible value is one of: full_content, one_per_au, one_per_techno")
    private ModuleGenerationType moduleGenerationType;

    public AnalyzeCommand(RestApiService restApiService, JobsService jobsService, ApplicationService applicationService) {
        super(restApiService, jobsService, applicationService);
    }

    @Override
    protected VersionInformation getMinVersion() {
        return null;
    }

    @Override
    protected Integer processCallCommand() throws Exception {
        if (StringUtils.isBlank(applicationName)) {
            log.error("No application name provided. Exiting.");
            return Constants.RETURN_APPLICATION_INFO_MISSING;
        }
        String applicationGuid;
        ApiInfoDto apiInfoDto = restApiService.getAipConsoleApiInfo();
        log.info("[Debug options] Show Sql is '{}'", showSql);
        log.info("[Debug options] AMT Profiling is '{}'", amtProfiling);

        try {
            log.info("Searching for application '{}' on AIP Console", applicationName);
            ApplicationDto app = applicationService.getApplicationFromName(applicationName);
            if (app == null || StringUtils.isEmpty(app.getGuid())) {
                log.error("Application '{}' was not found on AIP Console", applicationName);
                return Constants.RETURN_APPLICATION_NOT_FOUND;
            }
            applicationGuid = app.getGuid();
            Set<VersionDto> versions = applicationService.getApplicationVersion(applicationGuid);
            if (versions.isEmpty()) {
                log.error("No version for the given application. Make sure at least one version has been delivered");
                return Constants.RETURN_APPLICATION_NO_VERSION;
            }

            VersionDto versionToAnalyze;
            // Version with name provided
            if (StringUtils.isNotBlank(versionName)) {
                versionToAnalyze = versions.stream().filter(v -> StringUtils.equalsAnyIgnoreCase(v.getName(), versionName)).findFirst().orElse(null);
            } else {
                versionToAnalyze = versions
                        .stream()
                        .filter(v -> v.getStatus().ordinal() >= VersionStatus.DELIVERED.ordinal())
                        .max(Comparator.comparing(VersionDto::getVersionDate)).orElse(null);
            }
            if (versionToAnalyze == null) {
                String message = StringUtils.isBlank(versionName) ?
                        "Couldn't find a version to analyze. Make sure you have an accepted version OR a delivered version and pass the '--auto-deploy' parameter" :
                        "No version with name '" + versionName + "' could be found for application " + applicationName;
                log.error(message);
                return Constants.RETURN_APPLICATION_VERSION_NOT_FOUND;
            }
            // Deploy if auto deploy is true AND version to analyze has status DELIVERED (otherwise just do analysis)
            boolean deployFirst = versionToAnalyze.getStatus() == VersionStatus.DELIVERED;

            JobRequestBuilder builder = JobRequestBuilder.newInstance(applicationGuid, null, JobType.ANALYZE, app.getCaipVersion())
                    .startStep(deployFirst ? Constants.ACCEPTANCE_STEP_NAME : Constants.ANALYZE)
                    .nodeName(app.getTargetNode());

            if (withSnapshot) {
                boolean forcedConsolidation = processImaging || consolidation;
                builder.processImaging(processImaging)
                        .snapshotName(String.format("Snapshot-%s", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date())))
                        .uploadApplication(forcedConsolidation);
                //Snapshot required now see whether we upload application or not
                if (!forcedConsolidation) {
                    log.info("The snapshot {} for application will be taken but will not be published.", applicationName);
                    builder.endStep(Constants.SNAPSHOT_INDICATOR);
                }
            } else {
                builder.endStep(Constants.ANALYZE);
            }

            applicationService.updateModuleGenerationType(applicationGuid, builder, moduleGenerationType, false);

            builder.versionName(versionToAnalyze.getName())
                    .versionGuid(versionToAnalyze.getGuid())
                    .releaseAndSnapshotDate(new Date());

            //==============
            // Ony set debug option when ON. Run Analysis always consider these options as OFF(disabled)
            //==============
            DebugOptionsDto oldDebugOptions = applicationService.getDebugOptions(applicationGuid);
            if (showSql) {
                applicationService.updateShowSqlDebugOption(applicationGuid, showSql);
            }
            if (amtProfiling) {
                applicationService.updateShowSqlDebugOption(applicationGuid, amtProfiling);
            }

            log.info("Running analysis for application '{}' with version '{}'", applicationName, versionToAnalyze.getName());
            String jobGuid = jobsService.startJob(builder);
            Thread shutdownHook = getShutdownHookForJobGuid(jobGuid);

            Runtime.getRuntime().addShutdownHook(shutdownHook);
            JobExecutionDto jobStatus = jobsService.pollAndWaitForJobFinished(jobGuid, Function.identity(), sharedOptions.isVerbose());
            Runtime.getRuntime().removeShutdownHook(shutdownHook);

            DebugOptionsDto debugOptions = applicationService.getDebugOptions(applicationGuid);
            applicationService.resetDebugOptions(applicationGuid, oldDebugOptions);
            if (JobState.COMPLETED == jobStatus.getState()) {
                if (debugOptions.isActivateAmtMemoryProfile()) {
                    log.info("[Debug options] Amt Profiling file download URL: {}",
                            sharedOptions.getFullServerRootUrl() + ApiEndpointHelper.getAmtProfilingDownloadUrl(applicationGuid));
                }
                log.info("Application Analysis completed successfully");
                return Constants.RETURN_OK;
            }

            log.error("Analysis did not complete. Status is '{}' on step '{}'", jobStatus.getState(), jobStatus.getCurrentStep());
            return jobStatus.getState() == JobState.CANCELED ? Constants.RETURN_JOB_CANCELED : Constants.RETURN_JOB_FAILED;
        } catch (ApplicationServiceException e) {
            return Constants.RETURN_APPLICATION_INFO_MISSING;
        } catch (JobServiceException e) {
            return Constants.RETURN_JOB_POLL_ERROR;
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

    @Override
    public SharedOptions getSharedOptions() {
        return sharedOptions;
    }
}
