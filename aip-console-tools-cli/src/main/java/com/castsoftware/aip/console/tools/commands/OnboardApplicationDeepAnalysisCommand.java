package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.DeepAnalyzeProperties;
import com.castsoftware.aip.console.tools.core.dto.ModuleGenerationType;
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
public class OnboardApplicationDeepAnalysisCommand extends BasicCallable {
    @CommandLine.Option(names = {"-n", "--app-name"},
            paramLabel = "APPLICATION_NAME",
            description = "The Name of the application to analyze",
            required = true)
    private String applicationName;

    @CommandLine.Option(names = {"-S", "--snapshot-name"},
            paramLabel = "SNAPSHOT_NAME",
            description = "The name of the snapshot to create")
    private String snapshotName;

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
        DeepAnalyzeProperties deepAnalyzeProperties = DeepAnalyzeProperties.builder()
                .applicationName(applicationName)
                .moduleGenerationType(moduleGenerationType)
                .snapshotName(snapshotName)
                .sleepDuration(sharedOptions.getSleepDuration())
                .verbose(sharedOptions.isVerbose())
                .logPollingProvider(new CliLogPollingProviderImpl(jobsService, getSharedOptions().isVerbose(), getSharedOptions().getSleepDuration()))
                .build();
        return applicationService.deepAnalyze(deepAnalyzeProperties);
    }

    @Override
    protected VersionInformation getMinVersion() {
        return MIN_VERSION;
    }

}
