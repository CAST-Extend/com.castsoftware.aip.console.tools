package com.castsoftware.uc.aip.console.tools.commands;

import com.castsoftware.uc.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.uc.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.uc.aip.console.tools.core.exceptions.ApiKeyMissingException;
import com.castsoftware.uc.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.uc.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.uc.aip.console.tools.core.exceptions.UploadException;
import com.castsoftware.uc.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.uc.aip.console.tools.core.services.ChunkedUploadService;
import com.castsoftware.uc.aip.console.tools.core.services.JobsService;
import com.castsoftware.uc.aip.console.tools.core.services.RestApiService;
import com.castsoftware.uc.aip.console.tools.core.utils.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Component
@CommandLine.Command(
        name = "AddVersion",
        mixinStandardHelpOptions = true,
        aliases = {"add"},
        description = "Creates a new version for an application on AIP Console"
)
@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddVersionCommand implements Callable<Integer> {

    @Autowired
    private RestApiService restApiService;

    @Autowired
    private JobsService jobsService;

    @Autowired
    private ChunkedUploadService uploadService;

    @Autowired
    private ApplicationService applicationService;

    @CommandLine.Mixin
    private SharedOptions sharedOptions;

    /*
     * options for the upload and job startup
     */

    /**
     * The application name to look for on AIP Console
     */
    @CommandLine.Option(names = {"-n", "--app-name"}, paramLabel = "APPLICATION_NAME", description = "The Name of the application to rescan")
    private String applicationName;
    /**
     * The application GUID  on AIP Console
     */
    @CommandLine.Option(names = {"-a", "--app-guid"}, paramLabel = "APPLICATION_GUID", description = "The GUID of the application to rescan")
    private String applicationGuid;
    /**
     * A File that will be uploaded to AIP Console for the given application
     */
    @CommandLine.Option(names = {"-f", "--file"}, paramLabel = "FILE", description = "The ZIP file containing the source to rescan", required = true)
    private File archiveFilePath;
    /**
     * The Name fo the version from the command line
     */
    @CommandLine.Option(names = {"-v", "--version-name"}, paramLabel = "VERSION-NAME", description = "The name of the version to create")
    private String versionName;
    /**
     * Whether or not to clone previous version
     */
    @CommandLine.Option(names = {"-c", "--clone", "--rescan"}, description = "Clones the latest version configuration instead of creating a new application")
    private boolean cloneVersion = false;
    /**
     * Whether or not to automatically create the application before Adding a version (if the application could not be found)
     */
    @CommandLine.Option(names = "--auto-create", description = "If the given application name doesn't exist on the target server, it'll be automatically created before creating a new version")
    private boolean autoCreate = false;

    @CommandLine.Unmatched
    private List<String> unmatchedOptions;

    @Override
    public Integer call() {

        try {
            restApiService.setTimeout(sharedOptions.getTimeout(), TimeUnit.SECONDS);
            restApiService.validateUrlAndKey(sharedOptions.getFullServerRootUrl(), sharedOptions.getUsername(), sharedOptions.getApiKeyValue());
        } catch (ApiKeyMissingException e) {
            return Constants.RETURN_NO_PASSWORD;
        } catch (ApiCallException e) {
            return Constants.RETURN_LOGIN_ERROR;
        }

        if (StringUtils.isBlank(applicationName) && StringUtils.isBlank(applicationGuid)) {
            log.error("No application name or application guid provided. Exiting.");
            return Constants.RETURN_APPLICATION_INFO_MISSING;
        }

        try {
            if (StringUtils.isBlank(applicationGuid)) {
                log.info("Search for application '{}' or AIP Console", applicationName);
                applicationGuid = applicationService.getOrCreateApplicationFromName(applicationName, autoCreate);
                if (StringUtils.isBlank(applicationGuid)) {
                    String message;
                    if (autoCreate) {
                        message = "Creation of the application '{}' failed on AIP Console";
                    } else {
                        message = "Application '{}' was not found on AIP Console";
                    }
                    log.error(message, applicationName);
                    return Constants.RETURN_APPLICATION_NOT_FOUND;
                }
            }

            if (!uploadService.uploadFile(applicationGuid, archiveFilePath)) {
                log.error("Upload wasn't transmitted but was not considered complete by server. Check the file you provided wasn't modified since the start of the CLI");
                return Constants.RETURN_UPLOAD_ERROR;
            }

            String jobGuid = jobsService.startAddVersionJob(applicationGuid, archiveFilePath.getName(), versionName, new Date(), cloneVersion);
            JobState jobState = jobsService.pollAndWaitForJobFinished(jobGuid);
            if (JobState.COMPLETED == jobState) {
                log.info("Job completed successfully.");
                return Constants.RETURN_OK;
            }

            log.error("Job was not completed successfully. Finished with status '{}'", jobState.toString());
            return Constants.RETURN_JOB_FAILED;

        } catch (ApplicationServiceException e) {
            return Constants.RETURN_APPLICATION_INFO_MISSING;
        } catch (UploadException e) {
            log.error("Error occurred while attempting to upload the given file.", e);
            return Constants.RETURN_UPLOAD_ERROR;
        } catch (JobServiceException e) {
            return Constants.RETURN_JOB_POLL_ERROR;
        }
    }
}
