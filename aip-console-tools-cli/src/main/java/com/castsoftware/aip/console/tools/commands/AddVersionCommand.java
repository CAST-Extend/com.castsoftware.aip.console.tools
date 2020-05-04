package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.jobs.FileCommandRequest;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApiKeyMissingException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.UploadException;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.ChunkedUploadService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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
    private File filePath;
    /**
     * The Name fo the version from the command line
     */
    @CommandLine.Option(names = {"-v", "--version-name"}, paramLabel = "VERSION_NAME", description = "The name of the version to create")
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

    @CommandLine.Option(names = "--enable-security-dataflow", description = "If defined, this will activate the security dataflow for this version")
    private boolean enableSecurityDataflow = false;

    /**
     * The name of the target node where application will be created. Only used if --auto-create is true and the application doesn't exists
     */
    @CommandLine.Option(names = "--node-name", paramLabel = "NODE_NAME", description = "The name of the node on which the application will be created. Ignored if no --auto-create or the application already exists.")
    private String nodeName;

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

        if (StringUtils.isBlank(applicationName) && StringUtils.isBlank(applicationGuid)) {
            log.error("No application name or application guid provided. Exiting.");
            return Constants.RETURN_APPLICATION_INFO_MISSING;
        }

        try {
            if (StringUtils.isBlank(applicationGuid)) {
                log.info("Searching for application '{}' on AIP Console", applicationName);
                applicationGuid = applicationService.getOrCreateApplicationFromName(applicationName, autoCreate);
                if (StringUtils.isBlank(applicationGuid)) {
                    String message = autoCreate ?
                            "Creation of the application '{}' failed on AIP Console" :
                            "Application '{}' was not found on AIP Console";
                    log.error(message, applicationName);
                    return Constants.RETURN_APPLICATION_NOT_FOUND;
                }
            }

            if (StringUtils.isEmpty(applicationName) && StringUtils.isNotEmpty(applicationGuid)) {
                applicationName = applicationService.getApplicationNameFromGuid(applicationGuid);
            }


            String sourceFileName;
            // means it is a subfolder inside the source.folder.location defined in Console
            if (!StringUtils.equalsAnyIgnoreCase(FilenameUtils.getExtension(filePath.getName()), "zip", "tar.gz")) {
                //call api to check if the folder exists
                try {
                    FileCommandRequest fileCommandRequest = FileCommandRequest.builder().command("LS").path("SOURCES:" + filePath.toPath().toString()).build();
                    restApiService.postForEntity("/api/applications/" + applicationGuid + "/server-folders", fileCommandRequest, String.class);
                    sourceFileName = "sources:" + filePath.toPath().toString();
                } catch (ApiCallException e) {
                    return Constants.RETURN_SOURCE_FOLDER_NOT_FOUND;
                }
            } else {
                sourceFileName = UUID.randomUUID().toString() + "." + getFileExtension(filePath.getName());
                try (InputStream stream = Files.newInputStream(filePath.toPath())) {
                    long fileSize = filePath.length();
                    if (!uploadService.uploadInputStream(applicationGuid, sourceFileName, fileSize, stream)) {
                        log.error("Local file fully uploaded, but AIP Console expects more content (fileSize on AIP Console not reached). Check the file you provided wasn't modified since the start of the CLI");
                        return Constants.RETURN_UPLOAD_ERROR;
                    }
                    sourceFileName = "upload:" + applicationName + "/main_sources";
                } catch (IOException e) {
                    log.error("Unable to read archive content to be uploaded.", e);
                    throw new UploadException(e);
                }

            }
            // check that the application actually has versions, otherwise it's just an add version job
            cloneVersion = cloneVersion && applicationService.applicationHasVersion(applicationGuid);

            String jobGuid = jobsService.startAddVersionJob(applicationGuid, applicationName, sourceFileName, versionName, new Date(), cloneVersion, enableSecurityDataflow);
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

    private static String getFileExtension(String filename) {
        if (StringUtils.endsWithIgnoreCase(filename, ".tar.gz")) {
            return "tar.gz";
        }
        return FilenameUtils.getExtension(filename);
    }
}
