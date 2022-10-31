package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApiKeyMissingException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.services.UploadService;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Component
@CommandLine.Command(
        name = "OnboardApplication",
        mixinStandardHelpOptions = true,
        aliases = {"Onboard-Application"},
        description = "Creates an application or uses an existing application to manage source code using a modern on-boarding workflow in CAST Imaging Console."
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
            description = "A local zip or tar.gz file OR a path to a folder on the node where the source if saved",
            required = true)
    private File filePath;

    @CommandLine.Option(names = "--node-name", paramLabel = "NODE_NAME",
            description = "The name of the node on which the application will be created.")
    private String nodeName;

    @CommandLine.Option(names = "--domain-name", paramLabel = "DOMAIN_NAME",
            description = "A domain is a group of applications. You may use domain to sort/filter applications. Will be created if it doesn't exists. No domain will be assigned if left empty.")
    private String domainName;

    @CommandLine.Mixin
    private SharedOptions sharedOptions;

    public OnboardApplicationCommand(RestApiService restApiService, JobsService jobsService, UploadService uploadService, ApplicationService applicationService) {
        super(restApiService, jobsService, uploadService, applicationService);
    }

    @Override
    public Integer call() throws Exception {
        if (StringUtils.isBlank(applicationName)) {
            log.error("Application name should not be empty.");
            return Constants.RETURN_APPLICATION_INFO_MISSING;
        }

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

        String applicationGuid;
        Thread shutdownHook = null;

        boolean firstScan = true;
        try {
            log.info("Searching for application '{}' on CAST Imaging Console", applicationName);
            ApplicationDto app = applicationService.getApplicationFromName(applicationName);
            if (app != null) {
                app = applicationService.getApplicationDetails(app.getGuid());
                firstScan = app.getVersion() == null || StringUtils.isAnyEmpty(app.getImagingTenant(), app.getVersion().getGuid());
            }
            log.info("About to trigger on-boarding workflow for: '{}' application", firstScan ? "First-scan/Refresh" : "Rescan");

            //on-boarding
            if (firstScan) {
                String sourcePath = uploadService.uploadFileForOnboarding(filePath);
                log.info("Prepare to onboard Application " + applicationName + " with sources: " + sourcePath);
                applicationGuid = applicationService.onboardApplication(applicationName, domainName, sharedOptions.isVerbose(), sourcePath);
                log.info("Application " + applicationName + " onboarded successfully: GUID= " + applicationGuid);
            }
/*
            applicationGuid = applicationService.getOrCreateApplicationFromName(applicationName, autoCreate, nodeName, domainName, cssServerName, sharedOptions.isVerbose());
            if (StringUtils.isBlank(applicationGuid)) {
                String message = "Creation of the application '{}' failed on AIP Console";
                log.error(message, applicationName);
                return Constants.RETURN_APPLICATION_NOT_FOUND;
            }

 */
        } catch (
                ApplicationServiceException e) {
            return Constants.RETURN_APPLICATION_INFO_MISSING;
        } /*catch (                UploadException e) {
            log.error("Error occurred while attempting to upload the given file.", e);
            return Constants.RETURN_UPLOAD_ERROR;
        } catch (         JobServiceException e) {
            return Constants.RETURN_JOB_POLL_ERROR;
        } catch (                PackagePathInvalidException e) {
            log.error("Provided Path is invalid", e);
            return Constants.RETURN_JOB_FAILED;
        }*/ finally {
            // Remove shutdown hook after execution
            // This is to avoid exceptions during job execution to
            if (shutdownHook != null) {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            }
        }

        return null;
    }
}
