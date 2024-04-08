package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.ExclusionRuleType;
import com.castsoftware.aip.console.tools.core.dto.FastScanProperties;
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
        //edit description to match v3
        description = "Creates an application or uses an existing application to manage source code using a modern workflow in CAST Imaging Console."
)
@Slf4j
@Getter
@Setter
public class FastScanCommand extends BasicCallable {
    @CommandLine.Option(names = {"-n", "--app-name"},
            paramLabel = "APPLICATION_NAME",
            description = "The Name of the application to scan",
            required = true)
    private String applicationName;

    @CommandLine.Option(names = {"-f", "--file-path"},
            paramLabel = "FILE_PATH",
            description = "A local .zip, .tar or .gz file OR a path to a folder on the node where the source is saved(Ensure your file contains: database scripts, properties files, libraries or archives, source code, etc.)")
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
    private static final VersionInformation MIN_VERSION = VersionInformation.fromVersionString("3.0.0");

    public FastScanCommand(RestApiService restApiService, JobsService jobsService, UploadService uploadService, ApplicationService applicationService) {
        super(restApiService, jobsService, uploadService, applicationService);
    }

    @Override
    public Integer processCallCommand() throws Exception {
        if (StringUtils.isBlank(applicationName)) {
            log.error("Application name should not be empty.");
            return Constants.RETURN_APPLICATION_INFO_MISSING;
        }

        if (filePath == null) {
            log.error("A valid file path required to perform the Fast Scan operation");
            return Constants.RETURN_MISSING_FILE;
        }
        FastScanProperties fastScanProperties = FastScanProperties.builder()
                .applicationName(applicationName)
                .exclusionPatterns(exclusionPatterns)
                .exclusionRules(exclusionRules)
                .filePath(filePath)
                .verbose(sharedOptions.isVerbose())
                .sleepDuration(sharedOptions.getSleepDuration())
                .logPollingProvider(new CliLogPollingProviderImpl(jobsService,
                        getSharedOptions().isVerbose(), getSharedOptions().getSleepDuration()))
                .build();
        return applicationService.fastScan(fastScanProperties);
    }
    
    @Override
    protected VersionInformation getMinVersion() {
        return MIN_VERSION;
    }
}
