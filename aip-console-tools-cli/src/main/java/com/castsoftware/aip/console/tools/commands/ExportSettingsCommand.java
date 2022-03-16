package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.export.ExportApplicationsRequest;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApiKeyMissingException;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @Autowired
    private ApplicationService applicationService;

    /**
     * A File that represents the v1 imported settings in Json format
     */
    @CommandLine.Option(names = {"-f", "--file"}, paramLabel = "FILE"
            , description = "Full path to the JSON file used to store exported settings results."
            , required = true)
    private File filePath;

    @CommandLine.Option(names = {"-apps", "--appList"}
            , description = "Applications to be exported: semicolon-separated string list."
            , required = true)
    private String appList;

    @CommandLine.Mixin
    private SharedOptions sharedOptions;

    private static final int EXPORT_SIZE_MAX = 5;
    private static final String NAMES_SEPARATOR = ";";

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
        if (StringUtils.isBlank(appList)) {
            log.error("Applications list should not be empty.");
            return Constants.RETURN_INVALID_PARAMETERS_ERROR;
        }

        Set<String> toExportedApps;
        String[] applicationsToExport = appList.split(NAMES_SEPARATOR);
        if (applicationsToExport.length == 0 || applicationsToExport.length > EXPORT_SIZE_MAX) {
            log.error(String.format("Applications list size must be between 1 and %d", EXPORT_SIZE_MAX));
            return Constants.RETURN_INVALID_PARAMETERS_ERROR;
        } else {
            Set<String> candidates = Arrays.stream(applicationsToExport).map(String::trim).collect(Collectors.toSet());
            StringBuilder sbInput = new StringBuilder();
            candidates.forEach(s -> sbInput.append(String.format("%n- %s", s)));
            log.info("Selected applications to export: {}", sbInput);

            toExportedApps = applicationService.findApplicationsByNames(candidates).stream().map(ApplicationDto::getName).collect(Collectors.toSet());

            StringBuilder sb = new StringBuilder();
            toExportedApps.forEach(s -> sb.append(String.format("%n- %s", s)));
            log.info("Existing applications to export: {}", sb);
            if (toExportedApps.isEmpty()) {
                log.error("Unable to find the selected application on the server");
                return Constants.RETURN_EXPORT_SETTINGS_ERROR;
            }
        }

        Path exportedSettingsPath = filePath.toPath();
        if (Files.isDirectory(filePath.toPath())) {
            exportedSettingsPath = filePath.toPath().resolve(Constants.DEFAULT_EXPORTED_SETTINGS_FILENAME);
        }

        if (Files.exists(exportedSettingsPath)) {
            log.error("The supplied file already exists: " + exportedSettingsPath);
            return Constants.RETURN_FILE_ALREADY_EXISTS;
        }

        ExportApplicationsRequest requestBody = new ExportApplicationsRequest(Collections.unmodifiableSet(toExportedApps));
        try {
            log.info("Starting export settings from {} ", sharedOptions.getFullServerRootUrl());
            String exportedSettings = restApiService.postForEntity("/api/export", requestBody, String.class);

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
