package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationOnboardingDto;
import com.castsoftware.aip.console.tools.core.dto.DeliveryConfigurationDto;
import com.castsoftware.aip.console.tools.core.dto.ExclusionRuleType;
import com.castsoftware.aip.console.tools.core.dto.Exclusions;
import com.castsoftware.aip.console.tools.core.dto.VersionStatus;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.UploadException;
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

import java.io.File;

@Component
@CommandLine.Command(
        name = "FastScan",
        mixinStandardHelpOptions = true,
        aliases = {"Fast-Scan"},
        description = "Creates an application or uses an existing application to manage source code using a modern workflow in CAST Imaging Console."
)
@Slf4j
@Getter
@Setter
public class OnboardApplicationFastScanCommand extends BasicCollable {
    @CommandLine.Option(names = {"-n", "--app-name"},
            paramLabel = "APPLICATION_NAME",
            description = "The Name of the application to analyze",
            required = true)
    private String applicationName;

    @CommandLine.Option(names = {"-f", "--file"},
            paramLabel = "FILE",
            description = "A local zip or tar.gz file OR a path to a folder on the node where the source if saved")
    private File filePath;

    @CommandLine.Option(names = "--node-name", paramLabel = "NODE_NAME",
            description = "The name of the node on which the application will be created.")
    private String nodeName;

    @CommandLine.Option(names = "--domain-name", paramLabel = "DOMAIN_NAME",
            description = "A domain is a group of applications. You may use domain to sort/filter applications. Will be created if it doesn't exists. No domain will be assigned if left empty.")
    private String domainName;

    @CommandLine.Option(names = {"-exclude", "--exclude-patterns"},
            description = "File patterns(glob pattern) to exclude in the delivery, separated with comma")
    private String exclusionPatterns;
    @CommandLine.Option(names = {"--exclusion-rules"}, split = ",", type = ExclusionRuleType.class
            , description = "Project's exclusion rules, separated with comma. Valid values: ${COMPLETION-CANDIDATES}")
    private ExclusionRuleType[] exclusionRules;

    @CommandLine.Mixin
    private SharedOptions sharedOptions;

    //This version can be null if failed to convert from string
    private static final VersionInformation MIN_VERSION = VersionInformation.fromVersionString("2.5.0");

    public OnboardApplicationFastScanCommand(RestApiService restApiService, JobsService jobsService, UploadService uploadService, ApplicationService applicationService) {
        super(restApiService, jobsService, uploadService, applicationService);
    }

    @Override
    public Integer processCallCommand() throws Exception {
        if (StringUtils.isBlank(applicationName)) {
            log.error("Application name should not be empty.");
            return Constants.RETURN_APPLICATION_INFO_MISSING;
        }

        if (filePath == null || !filePath.exists()) {
            log.error("A valid file path required to perform the FAST SCAN operation");
            return Constants.RETURN_MISSING_FILE;
        }

        String applicationGuid;
        Thread shutdownHook = null;
        boolean firstScan = true;
        boolean OnBoardingModeWasOn = false; //status before processing
        try {
            OnBoardingModeWasOn = applicationService.isOnboardingSettingsEnabled();
            if (!OnBoardingModeWasOn) {
                log.info("The 'Onboard Application' mode is OFF on CAST Imaging Console: Set it ON before proceed");
                //applicationService.setEnableOnboarding(true);
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

            log.info("About to trigger New workflow for: 'Fast-Scan'");
            //on-boarding
            ApplicationOnboardingDto applicationOnboardingDto;
            String caipVersion = app != null ? app.getCaipVersion() : null;
            String targetNode = app != null ? app.getTargetNode() : null;
            String sourcePath = uploadFile(existingAppGuid);

            CliLogPollingProviderImpl cliLogPolling = new CliLogPollingProviderImpl(jobsService, getSharedOptions().isVerbose());

            if (firstScan) {
                applicationGuid = existingAppGuid;
                if (onboardApplication) {
                    applicationGuid = applicationService.onboardApplication(applicationName, domainName, getSharedOptions().isVerbose(), sourcePath);
                    log.info("Onboard Application job has started: application GUID= " + applicationGuid);
                }

                //Refresh application information
                app = applicationService.getApplicationFromName(applicationName);
                caipVersion = app.getCaipVersion();
                targetNode = app.getTargetNode();

                applicationService.discoverApplication(applicationGuid, sourcePath,
                        StringUtils.isNotEmpty(applicationGuid) ? "" : "My version", caipVersion, targetNode, getSharedOptions().isVerbose(), cliLogPolling);
                log.info("Application " + applicationName + " onboarded/refreshed successfully: GUID= " + applicationGuid);

                applicationOnboardingDto = applicationService.getApplicationOnboarding(applicationGuid);
                caipVersion = applicationOnboardingDto.getCaipVersion();
                targetNode = applicationOnboardingDto.getTargetNode();
                existingAppGuid = applicationGuid;
            } else {
                //For RESCAN PROCESS: re-discover
                sourcePath = app.getVersion().getSourcePath();
                Exclusions exclusions = Exclusions.builder().excludePatterns(exclusionPatterns).build();
                if (exclusionRules != null && exclusionRules.length > 0) {
                    exclusions.setInitialExclusionRules(exclusionRules);
                }

                //discover-packages
                log.info("Preparing the Application Delivery Configuration");
                final DeliveryConfigurationDto[] deliveryConfig = new DeliveryConfigurationDto[1];
                String deliveryConfigurationGuid = applicationService.discoverPackagesAndCreateDeliveryConfiguration(existingAppGuid, sourcePath, exclusions,
                        VersionStatus.IMAGING_PROCESSED, true, (config) -> deliveryConfig[0] = config);
                DeliveryConfigurationDto deliveryConfiguration = deliveryConfig[0];
                log.info("Application Delivery Configuration done: GUID=" + deliveryConfigurationGuid);

                //rediscover-application
                log.info("Preparing for Rediscover Application action");
                applicationService.reDiscoverApplication(existingAppGuid, sourcePath, "", deliveryConfiguration,
                        caipVersion, targetNode, getSharedOptions().isVerbose(), cliLogPolling);
                log.info("Rediscover Application done successfully");
            }

            if (!applicationService.isImagingAvailable()) {
                log.info("The 'Deep Analysis' step has been disabled by user. To perform this step do configure CAST Imaging and use the dedicated CLI command");
                return Constants.RETURN_RUN_ANALYSIS_DISABLED;
            }
            //Run Analysis delegated to Deep-Analysis
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

    private String uploadFile(String existingAppGuid) throws UploadException {
        String uploadAction = StringUtils.isEmpty(existingAppGuid) ? "Discover sources" : "refresh sources content";
        log.info("Prepare to " + uploadAction + " for Application " + applicationName);
        String sourcePath = uploadService.uploadFileForOnboarding(filePath, existingAppGuid);
        log.info(uploadAction + " uploaded successfully: " + sourcePath);
        return sourcePath;
    }

    @Override
    protected VersionInformation getMinVersion() {
        return MIN_VERSION;
    }
}
