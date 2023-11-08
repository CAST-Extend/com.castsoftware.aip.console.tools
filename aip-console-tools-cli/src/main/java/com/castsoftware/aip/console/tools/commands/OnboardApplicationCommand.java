package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.DeepAnalyzeProperties;
import com.castsoftware.aip.console.tools.core.dto.ExclusionRuleType;
import com.castsoftware.aip.console.tools.core.dto.FastScanProperties;
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
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;

@Component
@CommandLine.Command(
        name = "OnboardApplication",
        mixinStandardHelpOptions = true,
        aliases = {"Onboard-Application"},
        description = "Creates an application with Fast-Scan to manage source code using a modern workflow. Performs the Deep-Analyze and then Publish data to CAST Imaging Console."
)
@Slf4j
@Getter
@Setter
public class OnboardApplicationCommand extends BasicCollable {
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

    @CommandLine.Option(names = {"-S", "--snapshot-name"},
            paramLabel = "SNAPSHOT_NAME",
            description = "The name of the snapshot to create")
    private String snapshotName;

    @CommandLine.Option(names = "--module-option"
            , description = "Generates a user defined module option for either technology module or analysis unit module. Possible value is one of: full_content, one_per_au, one_per_techno (default: ${DEFAULT-VALUE})")
    private ModuleGenerationType moduleGenerationType = ModuleGenerationType.FULL_CONTENT;

    @CommandLine.Mixin
    private SharedOptions sharedOptions;

    private static final VersionInformation MIN_VERSION = VersionInformation.fromVersionString("2.8.0");

    public OnboardApplicationCommand(RestApiService restApiService, JobsService jobsService, UploadService uploadService, ApplicationService applicationService) {
        super(restApiService, jobsService, uploadService, applicationService);
    }

    @Override
    protected Integer processCallCommand() throws Exception {

        if (filePath == null) {
            log.error("A valid file path required to perform the FAST SCAN operation");
            return Constants.RETURN_MISSING_FILE;
        }
        CliLogPollingProviderImpl logPollingProvider = new CliLogPollingProviderImpl(jobsService,
                getSharedOptions().isVerbose(), getSharedOptions().getSleepDuration());
        FastScanProperties fastScanProperties = FastScanProperties.builder()
                .applicationName(applicationName)
                .exclusionPatterns(exclusionPatterns)
                .exclusionRules(exclusionRules)
                .filePath(filePath)
                .verbose(sharedOptions.isVerbose())
                .sleepDuration(sharedOptions.getSleepDuration())
                .logPollingProvider(logPollingProvider)
                .build();

        int exitCode = applicationService.fastScan(fastScanProperties);
        if (exitCode == Constants.RETURN_OK) {
            //Deep Analyze
            DeepAnalyzeProperties deepAnalyzeProperties = DeepAnalyzeProperties.builder()
                    .applicationName(applicationName)
                    .moduleGenerationType(moduleGenerationType)
                    .snapshotName(snapshotName)
                    .sleepDuration(sharedOptions.getSleepDuration())
                    .verbose(sharedOptions.isVerbose())
                    .logPollingProvider(logPollingProvider)
                    .build();
            exitCode = applicationService.deepAnalyze(deepAnalyzeProperties);
            if (exitCode == Constants.RETURN_OK) {
                long duration = sharedOptions.getSleepDuration();
                boolean verbose = getSharedOptions().isVerbose();
                exitCode = applicationService.publishToImaging(applicationName, duration, verbose, logPollingProvider);
            }
        }
        return exitCode;
    }

    @Override
    protected VersionInformation getMinVersion() {
        return MIN_VERSION;
    }
}
