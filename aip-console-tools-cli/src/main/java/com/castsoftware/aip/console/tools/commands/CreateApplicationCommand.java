package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.services.UploadService;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.core.utils.VersionInformation;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.util.List;

@Component
@CommandLine.Command(
        name = "CreateApplication",
        mixinStandardHelpOptions = true,
        aliases = {"new"},
        description = "Creates a new application on AIP Console"
)
@Slf4j
@Getter
@Setter
public class CreateApplicationCommand extends BasicCallable {

    @CommandLine.Mixin
    private SharedOptions sharedOptions;

    /**
     * options for the upload and job startup
     */
    @CommandLine.Option(names = {"-n", "--app-name"}, paramLabel = "APPLICATION_NAME", description = "The name of the application to create", required = true)
    private String applicationName;

    @CommandLine.Option(names = "--node-name", paramLabel = "NODE_NAME", description = "The name of the node on which the application will be created.")
    private String nodeName;

    /**
     * Domain name
     */
    @CommandLine.Option(names = "--domain-name", paramLabel = "DOMAIN_NAME", description = "The name of the domain to assign to the application. Will be created if it doesn't exists. No domain will be assigned if left empty.")
    private String domainName;

    @CommandLine.Option(names = "--inplace-mode",
            description = "If true then no history will be kept for delivered sources." + " if specified without parameter: ${FALLBACK-VALUE}",
            fallbackValue = "true",
            hidden = true,
            hideParamSyntax = true)
    private boolean inPlaceMode = false;
    @CommandLine.Option(names = {"-no-history", "--no-version-history"},
            description = "If true then no history will be kept for delivered sources." + " if specified without parameter: ${FALLBACK-VALUE}",
            fallbackValue = "true")
    private boolean noVersionHistory = false;

    @CommandLine.Option(names = {"-css", "--css-server"}, description = "CSS Server name that will host the application data: Format will be host:port/databaseName ")
    private String cssServerName;

    @CommandLine.Unmatched
    private List<String> unmatchedOptions;

    protected CreateApplicationCommand(RestApiService restApiService, JobsService jobsService, UploadService uploadService, ApplicationService applicationService) {
        super(restApiService, jobsService, uploadService, applicationService);
    }

    @Override
    public SharedOptions getSharedOptions() {
        return sharedOptions;
    }

    @Override
    public Integer processCallCommand() throws Exception {
        log.info("Create application command has triggered with log output = '{}'", sharedOptions.isVerbose());
        //For backward compatibility
        boolean noHistory = noVersionHistory || inPlaceMode;
        if (noHistory) {
            log.info("The created application will have \"No Version History\"");
        }

        try {

            String jobGuid = jobsService.startCreateApplication(applicationName, nodeName, domainName, noHistory, null, cssServerName);
            log.info("Started job to create new application.");
            return jobsService.pollAndWaitForJobFinished(jobGuid, (jobDetails) -> {
                if (jobDetails.getState() != JobState.COMPLETED) {
                    log.error("Creation of the application failed with status '{}'", jobDetails.getState());
                    return jobDetails.getState() == JobState.CANCELED ? Constants.RETURN_JOB_CANCELED : Constants.RETURN_JOB_FAILED;
                }
                log.info("Application '{}' created successfully:  GUID is '{}'", jobDetails.getAppName(), jobDetails.getAppGuid());
                return Constants.RETURN_OK;
            }, sharedOptions.isVerbose());
        } catch (JobServiceException e) {
            return Constants.RETURN_JOB_FAILED;
        }
    }

    @Override
    protected VersionInformation getMinVersion() {
        return null;
    }
}
