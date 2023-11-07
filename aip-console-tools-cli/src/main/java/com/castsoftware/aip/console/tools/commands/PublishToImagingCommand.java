package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.services.UploadService;
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
        long duration = sharedOptions.getSleepDuration();
        boolean verbose = getSharedOptions().isVerbose();
        CliLogPollingProviderImpl logPollingProvider = new CliLogPollingProviderImpl(jobsService, verbose, duration);

        return applicationService.publishToImaging(applicationName, duration, verbose, logPollingProvider);
                
    }

    @Override
    protected VersionInformation getMinVersion() {
        return MIN_VERSION;
    }

    @Override
    public SharedOptions getSharedOptions() {
        return sharedOptions;
    }
}