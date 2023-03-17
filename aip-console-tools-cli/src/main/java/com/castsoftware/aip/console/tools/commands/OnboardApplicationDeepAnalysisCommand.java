package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.ModuleGenerationType;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.services.UploadService;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.core.utils.VersionInformation;
import com.castsoftware.aip.console.tools.providers.CliLogPollingProviderImpl;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "DeepAnalyze",
        mixinStandardHelpOptions = true,
        aliases = {"Deep-Analyze"},
        description = "Performs a Deep Analysis for an existing application using a modern workflow in CAST Imaging Console."
)
@Slf4j
@Getter
@Setter
public class OnboardApplicationDeepAnalysisCommand extends BasicCollable {
    @CommandLine.Option(names = {"-n", "--app-name"},
            paramLabel = "APPLICATION_NAME",
            description = "The Name of the application to analyze",
            required = true)
    private String applicationName;

    @CommandLine.Option(names = {"-S", "--snapshot-name"},
            paramLabel = "SNAPSHOT_NAME",
            description = "The name of the snapshot to create")
    private String snapshotName;

    @CommandLine.Option(names = {"--sleep-duration"},
            description = "Number of seconds used to refresh the ongoing job status. The default value is: ${DEFAULT-VALUE}",
            defaultValue = "15")
    private long sleepDuration;

    @CommandLine.Option(names = "--module-option"
            , description = "Generates a user defined module option for either technology module or analysis unit module. Possible value is one of: full_content, one_per_au, one_per_techno (default: ${DEFAULT-VALUE})")
    private ModuleGenerationType moduleGenerationType = ModuleGenerationType.FULL_CONTENT;

    @CommandLine.Mixin
    private SharedOptions sharedOptions;

    //This version can be null if failed to convert from string
    private static final VersionInformation MIN_VERSION = VersionInformation.fromVersionString("2.8.0");

    public OnboardApplicationDeepAnalysisCommand(RestApiService restApiService, JobsService jobsService, UploadService uploadService, ApplicationService applicationService) {
        super(restApiService, jobsService, uploadService, applicationService);
    }

    @Override
    public Integer processCallCommand() throws Exception {
        if (StringUtils.isBlank(applicationName)) {
            log.error("Application name should not be empty.");
            return Constants.RETURN_APPLICATION_INFO_MISSING;
        }

        log.info("Deep-Analysis args:");
        log.info(String.format("\tApplication: %s%n\tsnapshot name: %s%n\tmodule generation type: %s%n\tsleep: %d%n", applicationName, StringUtils.isEmpty(snapshotName) ? "Auto assigned" : snapshotName, moduleGenerationType.toString(), sleepDuration));

        Thread shutdownHook = null;
        try {
            boolean OnBoardingModeWasOn = applicationService.isOnboardingSettingsEnabled();
            if (!OnBoardingModeWasOn) {
                log.info("The 'Onboard Application' mode is OFF on CAST Imaging Console: Set it ON before proceed");
                return Constants.RETURN_ONBOARD_APPLICATION_DISABLED;
            }

            log.info("Searching for application '{}' on CAST Imaging Console", applicationName);
            String existingAppGuid = null;
            ApplicationDto app = applicationService.getApplicationFromName(applicationName);
            if (app != null) {
                existingAppGuid = app.getGuid();
                app = applicationService.getApplicationDetails(existingAppGuid);
            }

            boolean deepAnalysisCondition = (app != null) && app.isOnboarded();
            if (!deepAnalysisCondition) {
                if (app != null && !app.isOnboarded()) {
                    log.info("The existing application has not been created using the Fast-Scan workflow.\n" +
                            "The 'Deep-Analysis' operation will not be applied");
                    return Constants.RETURN_ONBOARD_DEEP_ANALYSIS_FORBIDDEN;
                }

                log.error("Unable to trigger Deep-Analysis. The actual conditions required Fast-Scan to be running first.");
                return Constants.RETURN_ONBOARD_FAST_SCAN_REQUIRED;
            }

            log.info("About to trigger new workflow for: 'Deep-Analysis' ");
            if (StringUtils.isNotEmpty(getSnapshotName())) {
                log.info("  With snapshot name: " + getSnapshotName());
            }
            String caipVersion = app.getCaipVersion();
            String targetNode = app.getTargetNode();

            CliLogPollingProviderImpl cliLogPolling = new CliLogPollingProviderImpl(jobsService, getSharedOptions().isVerbose(), sleepDuration);

            //Run Analysis
            if (!applicationService.isImagingAvailable()) {
                log.info("The 'Deep Analysis' action is disabled because Imaging settings are missing from CAST AIP Console for Imaging.");
                return Constants.RETURN_RUN_ANALYSIS_DISABLED;
            }

            applicationService.runDeepAnalysis(existingAppGuid, targetNode, caipVersion, snapshotName, moduleGenerationType, getSharedOptions().isVerbose(), cliLogPolling);
        } catch (ApplicationServiceException e) {
            return Constants.RETURN_APPLICATION_INFO_MISSING;
        } finally {
            // Remove shutdown hook after execution
            // This is to avoid exceptions during job execution to
            if (shutdownHook != null) {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            }
        }

        return Constants.RETURN_OK;
    }

    @Override
    protected VersionInformation getMinVersion() {
        return MIN_VERSION;
    }

}
