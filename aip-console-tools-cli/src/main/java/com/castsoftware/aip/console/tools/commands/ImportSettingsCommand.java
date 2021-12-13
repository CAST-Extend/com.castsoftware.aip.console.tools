package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApiKeyMissingException;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.services.UploadService;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileUploadBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Component
@CommandLine.Command(
        name = "ImportSettings",
        mixinStandardHelpOptions = true,
        aliases = {"import"},
        description = "Import settings, domains, applications and other resources from JSON file"
)
@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImportSettingsCommand implements Callable<Integer> {
    @Autowired
    private RestApiService restApiService;
    @Autowired
    private JobsService jobsService;
    @Autowired
    private UploadService uploadService;

    /**
     * A File that represents the v1 imported settings in Json format
     */
    @CommandLine.Option(names = {"-f", "--file"}, paramLabel = "FILE"
            , description = "Json file that represents the settings exported from previous version."
            , required = true)
    private File filePath;

    @CommandLine.Mixin
    private SharedOptions sharedOptions;

    @Override
    public Integer call() throws Exception {
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

        if (!filePath.exists()) {
            log.error("The supplied file does not exist '" + filePath.getName() + "'");
            return Constants.RETURN_MISSING_FILE;
        }

        Map<String, String> contentHeaderMap = new HashMap<>();
        contentHeaderMap.put(FileUploadBase.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        contentHeaderMap.put(FileUploadBase.CONTENT_DISPOSITION, "form-data; name=\"file\"; filename=\"" + filePath.getName() + "\"");

        Map<String, Map<String, String>> headers = new HashMap<>();
        headers.put("content", contentHeaderMap);

        try {
            restApiService.exchangeMultipartForEntity("POST", "/api/import", headers, filePath, Void.class);
        } catch (ApiCallException e) {
            log.error("Unable to import settings file: '" + filePath.getName() + "'", e);
            return Constants.RETURN_IMPORT_SETTINGS_ERROR;
        }

        return Constants.RETURN_OK;
    }
}