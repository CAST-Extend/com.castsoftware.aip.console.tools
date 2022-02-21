package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.export.ExportDto;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApiKeyMissingException;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.services.UploadService;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Component
@CommandLine.Command(
        name = "ExportSettings",
        mixinStandardHelpOptions = true,
        aliases = {"export"},
        description = "Export settings, domains, applications, and other resources to JSON file"
)
@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExportSettingsCommand implements Callable<Integer> {
    @Autowired
    private RestApiService restApiService;
    @Autowired
    private JobsService jobsService;
    @Autowired
    private UploadService uploadService;
    @Autowired
    private ObjectMapper mapper;

    /**
     * A File that represents the v1 imported settings in Json format
     */
    @CommandLine.Option(names = {"-f", "--file"}, paramLabel = "FILE"
            , description = "Full path to the JSON file used to store exported settings results."
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

        if (!filePath.toPath().isAbsolute()) {
            log.error("The file argument should be an absolute path.");
            return Constants.RETURN_INVALID_PARAMETERS_ERROR;
        }
        Path exportedSettingsPath = filePath.toPath();
        if (Files.isDirectory(filePath.toPath())) {
            exportedSettingsPath = filePath.toPath().resolve(Constants.DEFAULT_EXPORTED_SETTINGS_FILENAME);
        }

        if (Files.exists(exportedSettingsPath)) {
            log.error("The supplied file already exists: " + exportedSettingsPath);
            return Constants.RETURN_FILE_ALREADY_EXISTS;
        }

        try {
            log.info("Starting export settings from {} ", sharedOptions.getFullServerRootUrl());
            //Using String here will require to deserialize thazt string before importing to V2
            ExportDto exportedSettings = restApiService.getForEntity("/api/export", ExportDto.class);

            log.info("Saving exported settings results to {} ", exportedSettingsPath);
            mapper.writeValue(exportedSettingsPath.toFile(), exportedSettings);
            log.info("Export settings completed.");
        } catch (ApiCallException e) {
            log.error("Unable to export the settings to '" + exportedSettingsPath + "'", e);
            return Constants.RETURN_EXPORT_SETTINGS_ERROR;
        }

        return Constants.RETURN_OK;
    }
}
