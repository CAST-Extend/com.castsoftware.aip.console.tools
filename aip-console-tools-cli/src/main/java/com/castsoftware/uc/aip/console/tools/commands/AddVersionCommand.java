package com.castsoftware.uc.aip.console.tools.commands;

import com.castsoftware.uc.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.uc.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.uc.aip.console.tools.core.exceptions.ApiKeyMissingException;
import com.castsoftware.uc.aip.console.tools.core.exceptions.JobServiceException;
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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;

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

    @CommandLine.Mixin
    private SharedOptions sharedOptions;

    /**
     * options for the upload and job startup
     */
    @CommandLine.Option(names = {"-a", "--app-guid"}, paramLabel = "APPLICATION_GUID", description = "The GUID of the application to rescan", required = true)
    private String applicationGuid;
    @CommandLine.Option(names = {"-f", "--file"}, paramLabel = "FILE", description = "The ZIP file containing the source to rescan", required = true)
    private String archiveFilePath;
    @CommandLine.Option(names = {"-v", "--version-name"}, paramLabel = "VERSION-NAME", description = "The name of the version to create")
    private String versionName;
    @CommandLine.Option(names = {"-c", "--clone"}, description = "Clones the latest version configuration instead of creating a new application")
    private boolean cloneVersion = false;

    @Override
    public Integer call() throws Exception {

        try {
            restApiService.validateUrlAndKey(sharedOptions.getFullServerRootUrl(), sharedOptions.getUsername(), sharedOptions.getApiKeyValue());
        } catch (ApiKeyMissingException e) {
            return Constants.RETURN_NO_PASSWORD;
        } catch (ApiCallException e) {
            return Constants.RETURN_LOGIN_ERROR;
        }

        assert Files.exists(Paths.get(archiveFilePath));

        if (!uploadService.uploadFile(applicationGuid, archiveFilePath)) {
            return Constants.RETURN_UPLOAD_ERROR;
        }

        DateFormat formatVersionName = new SimpleDateFormat("yyMMdd.HHmmss");
        Date now = new Date();
        if (StringUtils.isBlank(versionName)) {
            versionName = "v" + formatVersionName.format(now);
        }

        try {
            String jobGuid = jobsService.startAddVersionJob(applicationGuid, archiveFilePath, versionName, now, cloneVersion);

            JobState jobState = jobsService.pollAndWaitForJobFinished(jobGuid);
            if (JobState.COMPLETED == jobState) {
                log.info("Job completed successfully.");
                return Constants.RETURN_OK;
            }
            log.error("Job was not completed successfully. Finished with status '{}'", jobState.toString());
            return Constants.RETURN_JOB_FAILED;
        } catch (JobServiceException e) {
            return Constants.RETURN_JOB_POLL_ERROR;
        }
    }
}
