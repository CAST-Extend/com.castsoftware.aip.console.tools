package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.BasicCollable;
import com.castsoftware.aip.console.tools.commands.OnboardApplicationFastScanCommand;
import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationOnboardingDto;
import com.castsoftware.aip.console.tools.core.dto.VersionDto;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ReflectionUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {AipConsoleToolsCliIntegrationTest.class})
@ActiveProfiles(TestConstants.PROFILE_INTEGRATION_TEST)
public class OnboardApplicationFastScanCommandIntegrationTest extends AipConsoleToolsCliBaseTest {
    @InjectMocks
    private OnboardApplicationFastScanCommand fastScanCommand;

    @Override
    protected void initializePrivateMocks() {
        assignMockedBeans(fastScanCommand);
    }

    @Override
    protected void cleanupTestCommand() {
        // ===================================
        //command not recreated between test.
        //So just clear the command as if it was brand newly created
        //The best way is to run java -jar .\aip-console-tools-cli.jar add ...
        // So that app creates new instances of commands.
        // Still this woks fine renewing parameters values each time.
        // Here only String types, but each test should set velues to requested ones
        // ===================================
        resetSharedOptions(fastScanCommand.getSharedOptions());
        fastScanCommand.setApplicationName(null);
        fastScanCommand.setDomainName(null);
        fastScanCommand.setFilePath(null);
        fastScanCommand.setNodeName(null);
    }

    @Override
    protected void assignMockedBeans(BasicCollable command) {
        super.assignMockedBeans(command);

        //We need upload for this test
        Class<? extends BasicCollable> commandClass = command.getClass();
        Field uploadServiceField = ReflectionUtils.findField(commandClass, "uploadService");
        ReflectionUtils.makeAccessible(uploadServiceField);
        ReflectionUtils.setField(uploadServiceField, command, uploadService);
    }

    private Path uploadPath;
    ApplicationDto applicationDto;

    @Override
    protected void additionalStartup() throws IOException {
        super.additionalStartup();

        uploadPath = folder.getRoot().toPath().resolve("upload");
        Files.createDirectories(uploadPath);
        applicationDto = ApplicationDto.builder()
                .guid(TestConstants.TEST_APP_GUID)
                .name(TestConstants.TEST_CREATRE_APP).build();
    }

    @Test
    public void testOnboardApplicationFastScan_WithNotCompatibleVersion() throws Exception {
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY, "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "-f", zippedSourcesPath.toString(),
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--node-name", TestConstants.TEST_NODE};

        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion("2.4.9-funcrel").build();
        doReturn(apiInfoDto).when(restApiService).getAipConsoleApiInfo();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();
        when(restApiService.getForEntity("/api/", ApiInfoDto.class)).thenReturn(apiInfoDto);

        runStringArgs(fastScanCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_SERVER_VERSION_NOT_COMPATIBLE));
    }

    @Test
    public void testOnboardApplicationFastScan_WithCompatibleVersion() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATRE_APP,
                "-f", zippedSourcesPath.toString(),
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--node-name", TestConstants.TEST_NODE};

        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion("2.8.0-SNAPSHOT-133").build();
        doReturn(apiInfoDto).when(restApiService).getAipConsoleApiInfo();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();
        when(restApiService.getForEntity("/api/", ApiInfoDto.class)).thenReturn(apiInfoDto);
        doThrow(ApiCallException.class).when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doThrow(ApplicationServiceException.class).when(applicationService).isOnboardingSettingsEnabled();
        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(applicationDto);
        ApplicationOnboardingDto onboardedAppDto = Mockito.mock(ApplicationOnboardingDto.class);
        when(onboardedAppDto.getCaipVersion()).thenReturn("8.3.45");
        when(applicationService.getApplicationOnboarding(TestConstants.TEST_APP_GUID)).thenReturn(onboardedAppDto);

        runStringArgs(fastScanCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_APPLICATION_INFO_MISSING));
    }

    @Test
    public void testOnboardApplicationFastScan_OnboardingDisabled() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATRE_APP,
                "-f", zippedSourcesPath.toString(),
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--node-name", TestConstants.TEST_NODE};

        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion("2.8.0-SNAPSHOT-133").build();
        doReturn(apiInfoDto).when(restApiService).getAipConsoleApiInfo();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();
        when(restApiService.getForEntity("/api/", ApiInfoDto.class)).thenReturn(apiInfoDto);
        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doReturn(false).when(applicationService).isOnboardingSettingsEnabled();

        runStringArgs(fastScanCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(fastScanCommand.getSharedOptions().getSleepDuration(), is(10L)); //default value taken
        assertThat(exitCode, is(Constants.RETURN_ONBOARD_APPLICATION_DISABLED));
    }


    @Test
    public void testOnboardApplicationFastScan_WithoutExistingVersion() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATRE_APP,
                "-f", zippedSourcesPath.toString(),
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--node-name", TestConstants.TEST_NODE,
                "--sleep-duration", "6"};

        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion("2.8.0-SNAPSHOT-133").build();
        doReturn(apiInfoDto).when(restApiService).getAipConsoleApiInfo();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();
        when(restApiService.getForEntity("/api/", ApiInfoDto.class)).thenReturn(apiInfoDto);
        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doReturn(true).when(applicationService).isOnboardingSettingsEnabled();

        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(applicationDto);
        applicationDto.setOnboarded(true);
        doReturn(applicationDto).when(applicationService).getApplicationDetails(TestConstants.TEST_APP_GUID);

        Path sourcesPath = uploadPath.resolve(TestConstants.TEST_CREATRE_APP).resolve("main_sources");
        doReturn(sourcesPath.toString()).when(uploadService).uploadFileForOnboarding(zippedSourcesPath.toFile(), TestConstants.TEST_APP_GUID);
        ApplicationOnboardingDto onboardedAppDto = Mockito.mock(ApplicationOnboardingDto.class);
        when(onboardedAppDto.getCaipVersion()).thenReturn("8.3.45");
        when(applicationService.getApplicationOnboarding(TestConstants.TEST_APP_GUID)).thenReturn(onboardedAppDto);
        //Should work without imaging
        when(applicationService.isImagingAvailable()).thenReturn(false);

        runStringArgs(fastScanCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(fastScanCommand.getSharedOptions().getSleepDuration(), is(6L));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    @Test
    public void testOnboardApplicationFastScan_OnAnExistingNonOnboardeApplication() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATRE_APP,
                "-f", zippedSourcesPath.toString(),
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--node-name", TestConstants.TEST_NODE,
                "--sleep-duration", "6"};

        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion("2.8.0-SNAPSHOT-133").build();
        doReturn(apiInfoDto).when(restApiService).getAipConsoleApiInfo();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();
        when(restApiService.getForEntity("/api/", ApiInfoDto.class)).thenReturn(apiInfoDto);
        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doReturn(true).when(applicationService).isOnboardingSettingsEnabled();

        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(applicationDto);
        applicationDto.setOnboarded(false);
        doReturn(applicationDto).when(applicationService).getApplicationDetails(TestConstants.TEST_APP_GUID);

        Path sourcesPath = uploadPath.resolve(TestConstants.TEST_CREATRE_APP).resolve("main_sources");
        doReturn(sourcesPath.toString()).when(uploadService).uploadFileForOnboarding(zippedSourcesPath.toFile(), TestConstants.TEST_APP_GUID);
        ApplicationOnboardingDto onboardedAppDto = Mockito.mock(ApplicationOnboardingDto.class);
        when(onboardedAppDto.getCaipVersion()).thenReturn("8.3.45");
        when(applicationService.getApplicationOnboarding(TestConstants.TEST_APP_GUID)).thenReturn(onboardedAppDto);

        runStringArgs(fastScanCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_ONBOARD_FAST_SCAN_FORBIDDEN));
    }

    @Test
    public void testOnboardApplicationFastScan_WithoutSourcesProvided() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATRE_APP,
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--node-name", TestConstants.TEST_NODE}; //default

        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion("2.8.0-SNAPSHOT-133").build();
        doReturn(apiInfoDto).when(restApiService).getAipConsoleApiInfo();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();
        when(restApiService.getForEntity("/api/", ApiInfoDto.class)).thenReturn(apiInfoDto);
        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doReturn(true).when(applicationService).isOnboardingSettingsEnabled();

        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(applicationDto);
        doReturn(applicationDto).when(applicationService).getApplicationDetails(TestConstants.TEST_APP_GUID);

        runStringArgs(fastScanCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_MISSING_FILE));
    }

    @Test
    public void testOnboardApplicationFastScan_OnRefreshSourcesContent() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATRE_APP,
                "-f", zippedSourcesPath.toString(),
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--node-name", TestConstants.TEST_NODE};

        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion("2.8.0-SNAPSHOT-133").build();
        doReturn(apiInfoDto).when(restApiService).getAipConsoleApiInfo();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();
        when(restApiService.getForEntity("/api/", ApiInfoDto.class)).thenReturn(apiInfoDto);
        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doReturn(true).when(applicationService).isOnboardingSettingsEnabled();

        //To trigger refresh sources content
        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(applicationDto);
        VersionDto existingVersion = Mockito.mock(VersionDto.class);
        when(existingVersion.getGuid()).thenReturn(TestConstants.TEST_OBR_VERSION_GUID);
        when(existingVersion.getName()).thenReturn(TestConstants.TEST_OBR_VERSION_NAME);
        when(existingVersion.isImagingDone()).thenReturn(true);
        applicationDto.setVersion(existingVersion);
        applicationDto.setImagingTenant("default");
        applicationDto.setOnboarded(true);
        doReturn(applicationDto).when(applicationService).getApplicationDetails(TestConstants.TEST_APP_GUID);

        Path sourcesPath = uploadPath.resolve(TestConstants.TEST_CREATRE_APP).resolve("main_sources");
        doReturn(sourcesPath.toString()).when(uploadService).uploadFileForOnboarding(zippedSourcesPath.toFile(), TestConstants.TEST_APP_GUID);
        ApplicationOnboardingDto onboardedAppDto = Mockito.mock(ApplicationOnboardingDto.class);
        when(onboardedAppDto.getCaipVersion()).thenReturn("8.3.45");
        when(applicationService.getApplicationOnboarding(TestConstants.TEST_APP_GUID)).thenReturn(onboardedAppDto);

        runStringArgs(fastScanCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }
}
