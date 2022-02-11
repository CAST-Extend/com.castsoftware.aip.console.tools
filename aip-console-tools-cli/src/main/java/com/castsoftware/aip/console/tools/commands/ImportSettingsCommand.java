package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.PendingResultDto;
import com.castsoftware.aip.console.tools.core.dto.importsettings.ApplicationImportResultDto;
import com.castsoftware.aip.console.tools.core.dto.importsettings.DomainImportResultDto;
import com.castsoftware.aip.console.tools.core.dto.importsettings.ImportResultDto;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApiKeyMissingException;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.services.UploadService;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.core.utils.FileUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.apache.commons.fileupload.FileUploadBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * A File that represents the v1 imported settings in Json format
     */
    @CommandLine.Option(names = {"-f", "--file"}, paramLabel = "FILE"
            , description = "Json file that represents the settings exported from previous version."
            , required = true)
    private File filePath;

    @CommandLine.Mixin
    private SharedOptions sharedOptions;
    private StringBuffer logStringBuffer = new StringBuffer();

    private String logMessage(String msg) {
        logStringBuffer.append(msg);
        logStringBuffer.append(System.lineSeparator());
        return msg;
    }

    @Override
    public Integer call() throws Exception {
        log.info(logMessage(String.format("Importing all settings an other resources from file: %s", filePath.getAbsolutePath())));
        Path importLogFilePath = Paths.get(filePath.getParent()).resolve(filePath.getName() + "-import-log.txt");
        log.info(" You can find the associated log file here: {}", importLogFilePath);

        try {
            if (sharedOptions.getTimeout() != Constants.DEFAULT_HTTP_TIMEOUT) {
                restApiService.setTimeout(sharedOptions.getTimeout(), TimeUnit.SECONDS);
            }
            restApiService.validateUrlAndKey(sharedOptions.getFullServerRootUrl(), sharedOptions.getUsername(), sharedOptions.getApiKeyValue());
        } catch (ApiKeyMissingException e) {
            logMessage("No password");
            FileUtils.writeToFile(importLogFilePath, logStringBuffer.toString());
            return Constants.RETURN_NO_PASSWORD;
        } catch (ApiCallException e) {
            logMessage("Login error");
            FileUtils.writeToFile(importLogFilePath, logStringBuffer.toString());
            return Constants.RETURN_LOGIN_ERROR;
        }

        if (!filePath.exists()) {
            log.error(logMessage("The supplied file does not exist '" + filePath.getName() + "'"));
            FileUtils.writeToFile(importLogFilePath, logStringBuffer.toString());
            return Constants.RETURN_MISSING_FILE;
        }

        Integer exitCode = Constants.RETURN_OK;
        Map<String, String> contentHeaderMap = new HashMap<>();
        contentHeaderMap.put(FileUploadBase.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        contentHeaderMap.put(FileUploadBase.CONTENT_DISPOSITION, "form-data; name=\"file\"; filename=\"" + filePath.getName() + "\"");

        Map<String, Map<String, String>> headers = new HashMap<>();
        headers.put("content", contentHeaderMap);

        try {
            Response resp = restApiService.exchangeMultipartForResponse("POST", "/api/import", headers, filePath);
            int status = resp.code();
            Response importReponse = null;
            if (status == 200) {
                importReponse = resp;
            } else if (status == 202) { //Result still pending
                PendingResultDto resultDto = restApiService.mapResponse(resp, PendingResultDto.class);
                while (status != 200) {
                    Response response = restApiService.exchangeForResponse("GET", "/api/pending-results/" + resultDto.getGuid(), null);
                    status = response.code();
                    if (status == 200) {
                        importReponse = response;
                        break;
                    }
                    Thread.sleep(5000);
                }
            } else { //other statuses
                log.error(logMessage("Some fields failed to be converted (see bellow)"));
                Set<FailedFieldsDto> failingFields = objectMapper.readValue(resp.body().string().getBytes(StandardCharsets.UTF_8), new TypeReference<Set<FailedFieldsDto>>() {
                });
                failingFields.stream().forEach(wrongField -> {
                    Map<String, Object> args = wrongField.getArguments();
                    args.keySet().stream().forEach(k -> {
                        log.error(logMessage(String.format("\t Field: %s reason %s", k, args.get(k))));
                    });
                });
            }

            boolean someThingImported = false;
            if (importReponse != null) {
                ImportResultDto importedDomains = restApiService.mapResponse(importReponse, new TypeReference<ImportResultDto>() {
                });
                if (importedDomains.getDomains() != null) {
                    importedDomains.getDomains().stream().forEach(this::reportImported);
                    someThingImported = true;
                }
            }
            if (!someThingImported) {
                log.error(logMessage("Nothing imported"));
            }
        } catch (ApiCallException e) {
            log.error(logMessage("Unable to import settings file: '" + filePath.getName() + "'"), e);
            exitCode = Constants.RETURN_IMPORT_SETTINGS_ERROR;
        }


        FileUtils.writeToFile(importLogFilePath, logStringBuffer.toString());
        return exitCode;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private static class FailedFieldsDto {
        private Map<String, Object> arguments = new HashMap<>();
    }

    private void reportImported(DomainImportResultDto importedDomain) {
        log.info(logMessage(String.format("Domain: %s", importedDomain.getName().equals(Constants.DEFAULT_DOMAIN) ? "Default" : importedDomain.getName())));
        importedDomain.getApplications().stream().forEach(this::printLogMessage);
    }

    private void printLogMessage(ApplicationImportResultDto app) {
        String message;
        if (app.isImported()) {
            message = String.format("       Application %s imported successfully", app.getAppName());
            log.info(message);
        } else {
            message = String.format("       Application %s failed to import. Reason %s.", app.getAppName()
                    , app.getError() != null ? app.getError().getDefaultMessage() : "No details");
            log.error(message);
        }
        logMessage(message);
    }
}
