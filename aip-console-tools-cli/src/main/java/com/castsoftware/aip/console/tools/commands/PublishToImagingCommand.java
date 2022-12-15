package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.VersionDto;
import com.castsoftware.aip.console.tools.core.dto.VersionStatus;
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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import picocli.CommandLine;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@CommandLine.Command(
        name = "PublishToImaging",
        mixinStandardHelpOptions = true,
        aliases = {"Publish-Imaging"},
        description = "Publish an existing application data to CAST Imaging."
)
@Slf4j
@Getter
@Setter
public class PublishToImagingCommand extends BasicCollable {
    @CommandLine.Option(names = {"-n", "--app-name"},
            paramLabel = "APPLICATION_NAME",
            description = "The Name of the application to analyze",
            required = true)
    private String applicationName;

    @CommandLine.Mixin
    private SharedOptions sharedOptions;
    private final VersionInformation MIN_VERSION = VersionInformation.fromVersionString("2.5.0");

    protected PublishToImagingCommand(RestApiService restApiService, JobsService jobsService, UploadService uploadService, ApplicationService applicationService) {
        super(restApiService, jobsService, uploadService, applicationService);
    }

    @Override
    protected Integer processCallCommand() throws Exception {
        log.info("Publishing application '{}' data to CAST Imaging with verbose= '{}'", getApplicationName(), getSharedOptions().isVerbose());
        Thread shutdownHook = null;
        try {
            log.info("Searching for application '{}' on CAST Imaging Console", getApplicationName());
            ApplicationDto applicationDto = applicationService.getApplicationFromName(getApplicationName());
            if (applicationDto == null) {
                log.error("No action to perform: application '{}' does not exist.", getApplicationName());
                return Constants.RETURN_APPLICATION_NOT_FOUND;
            }

            if (!applicationService.isOnboardingSettingsEnabled()) {
                log.info("The 'Onboard Application' mode is OFF on CAST Imaging Console: Set it ON before proceed");
                //applicationService.setEnableOnboarding(true);
                return Constants.RETURN_ONBOARD_APPLICATION_DISABLED;
            }
            Set<VersionDto> versions = applicationService.getApplicationVersion(applicationDto.getGuid());
            if (versions == null || versions.isEmpty()) {
                log.error("No version for the given application. Make sure at least one version has been delivered");
                return Constants.RETURN_APPLICATION_NO_VERSION;
            }

            applicationDto = applicationService.getApplicationDetails(applicationDto.getGuid());
            Set<String> statuses = EnumSet.of(VersionStatus.ANALYSIS_DATA_PREPARED, VersionStatus.IMAGING_PROCESSED,
                            VersionStatus.SNAPSHOT_DONE, VersionStatus.FULLY_ANALYZED, VersionStatus.ANALYZED).stream()
                    .map(VersionStatus::toString).collect(Collectors.toSet());
            VersionDto versionDto = applicationDto.getVersion();
            if (!statuses.contains(versionDto.getStatus().toString())) {
                log.error("Application version not in the status that allows application data to be published to CAST Imaging: actual status is " + versionDto.getStatus().toString());
                return Constants.RETURN_ONBOARD_VERSION_STATUS_INVALID;
            }

            CliLogPollingProviderImpl cliLogPolling = new CliLogPollingProviderImpl(jobsService, getSharedOptions().isVerbose());
            String appGuid = applicationService.publishToImaging(applicationDto.getGuid(), cliLogPolling);

            if (StringUtils.isEmpty(appGuid)) {
                return Constants.RETURN_ONBOARD_OPERATION_FAILED;
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