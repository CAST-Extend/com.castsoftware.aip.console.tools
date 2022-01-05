package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.ImportSettingsCommand;
import com.castsoftware.aip.console.tools.core.dto.ObjectErrorDto;
import com.castsoftware.aip.console.tools.core.dto.importsettings.ApplicationImportResultDto;
import com.castsoftware.aip.console.tools.core.dto.importsettings.DomainImportResultDto;
import com.castsoftware.aip.console.tools.core.dto.importsettings.ImportResultDto;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.castsoftware.aip.console.tools.TestConstants.TEST_SRC_FOLDER;
import static com.castsoftware.aip.console.tools.core.services.RestApiServiceImpl.JSON_MEDIA_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {AipConsoleToolsCliIntegrationTest.class})
@ActiveProfiles(TestConstants.PROFILE_INTEGRATION_TEST)
public class ImportSettingsCommandIntegrationTest extends AipConsoleToolsCliBaseTest {
    @Autowired
    private ImportSettingsCommand importSettingsCommand;
    ObjectMapper mapper = null;

    @Override
    protected void cleanupTestCommand() {
        resetSharedOptions(importSettingsCommand.getSharedOptions());
        importSettingsCommand.setFilePath(null);
    }

    @Test
    public void testImportSettingsCommand_WithValidParametersAndUnExistingFile() throws ApplicationServiceException {
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "-f", TEST_SRC_FOLDER
        };

        runStringArgs(importSettingsCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_MISSING_FILE));
    }

    @Test
    public void testImportSettingsCommand_WithValidParametersAndExistingFile() throws IOException, ApiCallException {
        Path exportedFromV1 = sflPath.resolve("V1-Exported-file.json");
        exportedFromV1.toFile().createNewFile();
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "-f", exportedFromV1.toString()
        };

        List<ApplicationImportResultDto> applications = new ArrayList<>();
        applications.add(new ApplicationImportResultDto("ems-01", true, null));
        ObjectErrorDto errorDto = new ObjectErrorDto();
        errorDto.setCode("databaseConnectionFailed");
        errorDto.setDefaultMessage("Database connection failed");
        applications.add(new ApplicationImportResultDto("monster-01", false, errorDto));
        DomainImportResultDto oneDomain = new DomainImportResultDto();
        oneDomain.setName("java");
        oneDomain.setApplications(applications);
        List<DomainImportResultDto> domains = new ArrayList<>();
        domains.add(oneDomain);
        ImportResultDto importedResults = new ImportResultDto();
        importedResults.setDomains(domains);

        Response theResponse = createResponse(200, importedResults);

        when(restApiService.exchangeMultipartForResponse(anyString(), anyString(), ArgumentMatchers.anyMap()
                , any(File.class))).thenReturn(theResponse);
        when(restApiService.mapResponse(any(Response.class), (TypeReference<ImportResultDto>) any())).thenReturn(importedResults);

        runStringArgs(importSettingsCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    private ObjectMapper getMapper() {
        if (mapper == null) {
            this.mapper = new ObjectMapper();
            this.mapper.registerModule(new JavaTimeModule());
            this.mapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
            this.mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
            this.mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
        return mapper;
    }

    private Response createResponse(int code, ImportResultDto bodyEntity) {
        HttpUrl mHttpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("example.com")
                .build();
        Request mRequest = new Request.Builder()
                .url(mHttpUrl)
                .build();
        Response.Builder builder = new Response.Builder()
                .request(mRequest)
                .protocol(Protocol.HTTP_1_1)
                .message("message")
                .code(code);
        MediaType jsonMediaType = MediaType.parse(JSON_MEDIA_TYPE);
        try {
            builder.body(ResponseBody.create(
                    jsonMediaType,
                    bodyEntity == null ? "" : getMapper().writeValueAsString(bodyEntity)));
        } catch (JsonProcessingException e) {
        }


        return builder.build();
    }

    @Test
    public void testImportSettingsCommand_ExistingFileWithApiException() throws IOException, ApiCallException {
        Path exportedFromV1 = sflPath.resolve("V1-Exported-file.json");
        exportedFromV1.toFile().createNewFile();
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "-f", exportedFromV1.toString()
        };


        when(restApiService.exchangeMultipartForResponse(anyString(), anyString(), ArgumentMatchers.<String, Map<String, String>>anyMap()
                , ArgumentMatchers.<File>any(File.class))).thenThrow(new ApiCallException(295, "Unable to execute multipart form data with provided content"));

        runStringArgs(importSettingsCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_IMPORT_SETTINGS_ERROR));
    }

    @Test
    public void testImportSettingsCommand_WithMissingFile() {
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "-f"
        };

        runStringArgs(importSettingsCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_INVALID_PARAMETERS_ERROR));
    }

}
