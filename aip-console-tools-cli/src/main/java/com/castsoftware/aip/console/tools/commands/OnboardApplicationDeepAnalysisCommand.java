package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
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
            description = "Amount of time  used to fetch the ongoing job status (specified in seconds ). The default value is: ${DEFAULT-VALUE}",
            defaultValue = "15")
    private long sleepDuration;

    @CommandLine.Mixin
    private SharedOptions sharedOptions;

    //This version can be null if failed to convert from string
    private static final VersionInformation MIN_VERSION = VersionInformation.fromVersionString("2.5.0");


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
        log.info(String.format("\tApplication: %s%n\tsnapshot name: %s%n\tsleep: %d%n", applicationName, StringUtils.isEmpty(snapshotName) ? "Auto assigned" : snapshotName, sleepDuration));

        Thread shutdownHook = null;
        boolean firstScan = true;
        try {
            boolean OnBoardingModeWasOn = applicationService.isOnboardingSettingsEnabled();
            if (!OnBoardingModeWasOn) {
                log.info("The 'Onboard Application' mode is OFF on CAST Imaging Console: Set it ON before proceed");
                return Constants.RETURN_ONBOARD_APPLICATION_DISABLED;
            }

            log.info("Searching for application '{}' on CAST Imaging Console", applicationName);
            boolean onboardApplication = false;
            String existingAppGuid = null;
            ApplicationDto app = applicationService.getApplicationFromName(applicationName);
            if (app != null) {
                existingAppGuid = app.getGuid();
                app = applicationService.getApplicationDetails(existingAppGuid);
                firstScan = app.getVersion() == null || !app.isOnboarded() || StringUtils.isEmpty(app.getSchemaPrefix());
            } else {
                onboardApplication = true;
            }

            boolean deepAnalysisCondition = !onboardApplication && (app.isOnboarded() || StringUtils.isNotEmpty(app.getSchemaPrefix()));
            if (!deepAnalysisCondition) {
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

            if (firstScan) {
                applicationService.runFirstScanApplication(existingAppGuid, targetNode, caipVersion, snapshotName, getSharedOptions().isVerbose(), cliLogPolling);
            } else {
                applicationService.runReScanApplication(existingAppGuid, targetNode, caipVersion, snapshotName, getSharedOptions().isVerbose(), cliLogPolling);
            }
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