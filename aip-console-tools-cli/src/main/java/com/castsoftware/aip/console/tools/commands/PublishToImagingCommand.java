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
import org.springframework.stereotype.Component;
import picocli.CommandLine;

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

            CliLogPollingProviderImpl cliLogPolling = new CliLogPollingProviderImpl(jobsService, getSharedOptions().isVerbose());
            applicationService.publishToImaging(applicationDto.getGuid(), cliLogPolling);

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