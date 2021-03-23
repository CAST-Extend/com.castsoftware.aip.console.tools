package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.NodeDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApiKeyMissingException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

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
@NoArgsConstructor
@AllArgsConstructor
public class CreateApplicationCommand implements Callable<Integer> {

    @Autowired
    private JobsService jobsService;

    @Autowired
    private RestApiService restApiService;

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
            description = "If checked then no history will be kept for delivered sources. In this case, only sources from a unique folder location will be processed.")
    private boolean inPlaceMode = false;

    @CommandLine.Unmatched
    private List<String> unmatchedOptions;

    @Override
    public Integer call() {
        try {
            if (sharedOptions.getTimeout() != Constants.DEFAULT_HTTP_TIMEOUT) {
                restApiService.setTimeout(sharedOptions.getTimeout(), TimeUnit.SECONDS);
            }
            restApiService.validateUrlAndKey(sharedOptions.getFullServerRootUrl(), sharedOptions.getUsername(), sharedOptions.getApiKeyValue());
        } catch (ApiKeyMissingException e) {
            return Constants.RETURN_NO_PASSWORD;
        } catch (ApiCallException e) {
            return Constants.RETURN_LOGIN_ERROR;
        }

        log.info("Create application command has triggered with log output = '{}'", sharedOptions.isVerbose());

        try {
            String nodeGuid = null;
            if (StringUtils.isNotBlank(nodeName)) {
                NodeDto[] nodes = restApiService.getForEntity("/api/nodes", NodeDto[].class);
                nodeGuid = Arrays.stream(nodes)
                        .filter(node -> StringUtils.equalsIgnoreCase(nodeName, node.getName()))
                        .map(NodeDto::getGuid)
                        .findFirst()
                        .orElse(null);
                if (nodeGuid == null) {
                    log.error("Node with name '%s' could not be found on AIP Console.");
                    return Constants.RETURN_APPLICATION_NOT_FOUND;
                }
            }
            String jobGuid = jobsService.startCreateApplication(applicationName, nodeGuid, domainName, inPlaceMode);
            log.info("Started job to create new application.");
            return jobsService.pollAndWaitForJobFinished(jobGuid, (jobDetails) -> {
                if (jobDetails.getState() != JobState.COMPLETED) {
                    log.error("Creation of the application failed with status '{}'", jobDetails.getState());
                    return Constants.RETURN_JOB_FAILED;
                }
                log.info("Application '{}' created successfully:  GUID is '{}'", jobDetails.getAppName(), jobDetails.getAppGuid());
                return Constants.RETURN_OK;
            }, sharedOptions.isVerbose());
        } catch (JobServiceException e) {
            return Constants.RETURN_JOB_FAILED;
        } catch (ApiCallException e) {
            log.error("Call to AIP Console resulted in an error.", e);
            return Constants.UNKNOWN_ERROR;
        }
    }
}
