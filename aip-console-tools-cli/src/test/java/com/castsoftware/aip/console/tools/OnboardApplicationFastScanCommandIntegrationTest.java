package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.BasicCallable;
import com.castsoftware.aip.console.tools.commands.FastScanCommand;
import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationOnboardingDto;
import com.castsoftware.aip.console.tools.core.dto.Applications;
import com.castsoftware.aip.console.tools.core.dto.DeliveryConfigurationDto;
import com.castsoftware.aip.console.tools.core.dto.Exclusions;
import com.castsoftware.aip.console.tools.core.dto.FastScanProperties;
import com.castsoftware.aip.console.tools.core.dto.VersionDto;
import com.castsoftware.aip.console.tools.core.dto.VersionStatus;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobExecutionDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.services.ApplicationServiceImpl;
import com.castsoftware.aip.console.tools.core.utils.ApiEndpointHelper;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
    private FastScanCommand fastScanCommand;
    @InjectMocks
    private ApplicationServiceImpl applicationServiceImpl;

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
        // Here only String types, but each test should set values to requested ones
        // ===================================
        resetSharedOptions(fastScanCommand.getSharedOptions());
        fastScanCommand.setApplicationName(null);
        fastScanCommand.setDomainName(null);
        fastScanCommand.setFilePath(null);
        fastScanCommand.setNodeName(null);
    }

    @Override
    protected void assignMockedBeans(BasicCallable command) {
        super.assignMockedBeans(command);

        //We need upload for this test
        Class<? extends BasicCallable> commandClass = command.getClass();
        Field uploadServiceField = ReflectionUtils.findField(commandClass, "uploadService");
        if (uploadServiceField != null) {
            ReflectionUtils.makeAccessible(uploadServiceField);
            ReflectionUtils.setField(uploadServiceField, command, uploadService);
        }
    }

    private Path uploadPath;
    ApplicationDto applicationDto;

    @Override
    protected void additionalStartup() throws IOException {
        super.additionalStartup();

        ReflectionTestUtils.setField(applicationServiceImpl, "restApiService", restApiService);
        ReflectionTestUtils.setField(applicationServiceImpl, "jobService", jobsService);
        ReflectionTestUtils.setField(applicationServiceImpl, "uploadService", uploadService);

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
        assertThat(exitCode, is(Constants.RETURN_BAD_SERVER_VERSION));
    }

    @Test
    public void testOnboardApplicationFastScan_WithCompatibleVersion() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATRE_APP,
                "-f", zippedSourcesPath.toString(),
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--node-name", TestConstants.TEST_NODE};

        doThrow(ApiCallException.class).when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doThrow(ApplicationServiceException.class).when(applicationService).isOnboardingSettingsEnabled();
        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(applicationDto);
        ApplicationOnboardingDto onboardedAppDto = Mockito.mock(ApplicationOnboardingDto.class);
        when(onboardedAppDto.getCaipVersion()).thenReturn("8.3.45");
        when(applicationService.getApplicationOnboarding(TestConstants.TEST_APP_GUID)).thenReturn(onboardedAppDto);

        doThrow(ApiCallException.class).when(restApiService).getForEntity(ApiEndpointHelper.getEnableOnboardingSettingsEndPoint(), Boolean.class);
        when(applicationService.fastScan(any(FastScanProperties.class))).then(i -> applicationServiceImpl.fastScan(i.getArgument(0)));

        runStringArgs(fastScanCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_APPLICATION_INFO_MISSING));
    }

    @Test
    public void testOnboardApplicationFastScan_WithRelativePath() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATRE_APP,
                "-f", sourceFolderPath.toString(),
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--node-name", TestConstants.TEST_NODE};

        doThrow(ApiCallException.class).when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doThrow(ApplicationServiceException.class).when(applicationService).isOnboardingSettingsEnabled();
        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(applicationDto);
        ApplicationOnboardingDto onboardedAppDto = Mockito.mock(ApplicationOnboardingDto.class);
        when(onboardedAppDto.getCaipVersion()).thenReturn("8.3.45");
        when(applicationService.getApplicationOnboarding(TestConstants.TEST_APP_GUID)).thenReturn(onboardedAppDto);

        doThrow(ApiCallException.class).when(restApiService).getForEntity(ApiEndpointHelper.getEnableOnboardingSettingsEndPoint(), Boolean.class);
        when(applicationService.fastScan(any(FastScanProperties.class))).then(i -> applicationServiceImpl.fastScan(i.getArgument(0)));

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

        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doReturn(false).when(applicationService).isOnboardingSettingsEnabled();

        when(restApiService.getForEntity(ApiEndpointHelper.getEnableOnboardingSettingsEndPoint(), Boolean.class)).thenReturn(false);
        when(applicationService.fastScan(any(FastScanProperties.class))).then(i -> applicationServiceImpl.fastScan(i.getArgument(0)));

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
        when(applicationService.discoverPackagesAndCreateDeliveryConfiguration(
                anyString(), anyString(), any(Exclusions.class), any(VersionStatus.class), anyBoolean(),
                ArgumentMatchers.any(), anyBoolean()))
                .then(invocationOnMock -> {
                    Consumer<DeliveryConfigurationDto> arg = invocationOnMock.getArgument(5);
                    arg.accept(DeliveryConfigurationDto.builder().build());
                    return "delivery-Configuration-G-U-I-D";
                });

        runStringArgs(fastScanCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(fastScanCommand.getSharedOptions().getSleepDuration(), is(6L));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    @Test
    public void testOnboardApplicationFastScan_OnAnExistingNonOnboardedApplication() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATRE_APP,
                "-f", zippedSourcesPath.toString(),
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--node-name", TestConstants.TEST_NODE,
                "--sleep-duration", "6"};

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

        Applications applications = new Applications();
        applications.setApplications(applicationDto != null ? Sets.newHashSet(applicationDto) : Sets.newHashSet());
        when(restApiService.getForEntity(ApiEndpointHelper.getApplicationsPath(), Applications.class)).thenReturn(applications);
        when(restApiService.getForEntity(ApiEndpointHelper.getEnableOnboardingSettingsEndPoint(), Boolean.class)).thenReturn(true);
        when(applicationService.fastScan(any(FastScanProperties.class))).then(i -> applicationServiceImpl.fastScan(i.getArgument(0)));

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

        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doReturn(true).when(applicationService).isOnboardingSettingsEnabled();

        //To trigger refresh sources content
        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(applicationDto);
        VersionDto existingVersion = Mockito.mock(VersionDto.class);
        when(existingVersion.getGuid()).thenReturn(TestConstants.TEST_OBR_VERSION_GUID);
        when(existingVersion.getName()).thenReturn(TestConstants.TEST_OBR_VERSION_NAME);
        when(existingVersion.isImagingDone()).thenReturn(true);
        when(existingVersion.getVersionDate()).thenReturn(LocalDateTime.now());
        when(existingVersion.getStatus()).thenReturn(VersionStatus.DELIVERED);

        applicationDto.setVersion(existingVersion);
        applicationDto.setImagingTenant("default");
        applicationDto.setOnboarded(true);
        doReturn(applicationDto).when(applicationService).getApplicationDetails(TestConstants.TEST_APP_GUID);

        Path sourcesPath = uploadPath.resolve(TestConstants.TEST_CREATRE_APP).resolve("main_sources");
        doReturn(sourcesPath.toString()).when(uploadService).uploadFileForOnboarding(zippedSourcesPath.toFile(), TestConstants.TEST_APP_GUID);
        ApplicationOnboardingDto onboardedAppDto = Mockito.mock(ApplicationOnboardingDto.class);
        when(onboardedAppDto.getCaipVersion()).thenReturn("8.3.45");
        when(onboardedAppDto.getTargetNode()).thenReturn("Node");
        when(applicationService.getApplicationOnboarding(TestConstants.TEST_APP_GUID)).thenReturn(onboardedAppDto);
        when(applicationService.discoverPackagesAndCreateDeliveryConfiguration(
                anyString(), anyString(), any(Exclusions.class), any(VersionStatus.class), anyBoolean(),
                ArgumentMatchers.any(), anyBoolean()))
                .then(invocationOnMock -> {
                    Consumer<DeliveryConfigurationDto> arg = invocationOnMock.getArgument(5);
                    arg.accept(DeliveryConfigurationDto.builder().build());
                    return "delivery-Configuration-G-U-I-D";
                });

        when(restApiService.getForEntity(ApiEndpointHelper.getApplicationOnboardingPath(TestConstants.TEST_APP_GUID), ApplicationOnboardingDto.class)).thenReturn(onboardedAppDto);
        when(restApiService.getForEntity(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(Sets.newHashSet(existingVersion));
        Applications applications = new Applications();
        applications.setApplications(applicationDto != null ? Sets.newHashSet(applicationDto) : Sets.newHashSet());
        when(restApiService.getForEntity(ApiEndpointHelper.getApplicationsPath(), Applications.class)).thenReturn(applications);
        when(restApiService.getForEntity(ApiEndpointHelper.getEnableOnboardingSettingsEndPoint(), Boolean.class)).thenReturn(true);
        when(applicationService.fastScan(any(FastScanProperties.class))).then(i -> applicationServiceImpl.fastScan(i.getArgument(0)));

        when(jobsService.startFastScan(anyString(), anyString(), anyString(), any(DeliveryConfigurationDto.class), anyString(), anyString())).thenReturn(TestConstants.TEST_JOB_GUID);

        JobExecutionDto jobStatus = new JobExecutionDto();
        jobStatus.setAppGuid(TestConstants.TEST_APP_GUID);
        jobStatus.setState(JobState.COMPLETED);
        jobStatus.setCreatedDate(new Date());
        jobStatus.setAppName(TestConstants.TEST_CREATRE_APP);
        when(jobsService.pollAndWaitForJobFinished(TestConstants.TEST_JOB_GUID, Function.identity(), true)).thenReturn(jobStatus);
        when(jobsService.pollAndWaitForJobFinished(anyString(), any(Consumer.class), any(Consumer.class), any(Function.class), any(Supplier.class))).thenReturn(JobState.COMPLETED.toString());

        runStringArgs(fastScanCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    @Test
    public void testFastScan_JobExecutionFailed() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATRE_APP,
                "-f", zippedSourcesPath.toString(),
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--node-name", TestConstants.TEST_NODE};

        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doReturn(true).when(applicationService).isOnboardingSettingsEnabled();

        //To trigger refresh sources content
        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(applicationDto);
        VersionDto existingVersion = Mockito.mock(VersionDto.class);
        when(existingVersion.getGuid()).thenReturn(TestConstants.TEST_OBR_VERSION_GUID);
        when(existingVersion.getName()).thenReturn(TestConstants.TEST_OBR_VERSION_NAME);
        when(existingVersion.isImagingDone()).thenReturn(true);
        when(existingVersion.getVersionDate()).thenReturn(LocalDateTime.now());
        when(existingVersion.getStatus()).thenReturn(VersionStatus.DELIVERED);

        applicationDto.setVersion(existingVersion);
        applicationDto.setImagingTenant("default");
        applicationDto.setOnboarded(true);
        doReturn(applicationDto).when(applicationService).getApplicationDetails(TestConstants.TEST_APP_GUID);

        Path sourcesPath = uploadPath.resolve(TestConstants.TEST_CREATRE_APP).resolve("main_sources");
        doReturn(sourcesPath.toString()).when(uploadService).uploadFileForOnboarding(zippedSourcesPath.toFile(), TestConstants.TEST_APP_GUID);
        ApplicationOnboardingDto onboardedAppDto = Mockito.mock(ApplicationOnboardingDto.class);
        when(onboardedAppDto.getCaipVersion()).thenReturn("8.3.45");
        when(onboardedAppDto.getTargetNode()).thenReturn("Node");
        when(applicationService.getApplicationOnboarding(TestConstants.TEST_APP_GUID)).thenReturn(onboardedAppDto);
        when(applicationService.discoverPackagesAndCreateDeliveryConfiguration(
                anyString(), anyString(), any(Exclusions.class), any(VersionStatus.class), anyBoolean(),
                ArgumentMatchers.any(), anyBoolean()))
                .then(invocationOnMock -> {
                    Consumer<DeliveryConfigurationDto> arg = invocationOnMock.getArgument(5);
                    arg.accept(DeliveryConfigurationDto.builder().build());
                    return "delivery-Configuration-G-U-I-D";
                });

        when(restApiService.getForEntity(ApiEndpointHelper.getApplicationOnboardingPath(TestConstants.TEST_APP_GUID), ApplicationOnboardingDto.class)).thenReturn(onboardedAppDto);
        when(restApiService.getForEntity(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(Sets.newHashSet(existingVersion));
        Applications applications = new Applications();
        applications.setApplications(applicationDto != null ? Sets.newHashSet(applicationDto) : Sets.newHashSet());
        when(restApiService.getForEntity(ApiEndpointHelper.getApplicationsPath(), Applications.class)).thenReturn(applications);
        when(restApiService.getForEntity(ApiEndpointHelper.getEnableOnboardingSettingsEndPoint(), Boolean.class)).thenReturn(true);
        when(applicationService.fastScan(any(FastScanProperties.class))).then(i -> applicationServiceImpl.fastScan(i.getArgument(0)));

        when(jobsService.startFastScan(anyString(), anyString(), anyString(), any(DeliveryConfigurationDto.class), anyString(), anyString())).thenReturn(TestConstants.TEST_JOB_GUID);

        JobExecutionDto jobStatus = new JobExecutionDto();
        jobStatus.setAppGuid(TestConstants.TEST_APP_GUID);
        jobStatus.setState(JobState.FAILED);
        jobStatus.setCreatedDate(new Date());
        jobStatus.setAppName(TestConstants.TEST_CREATRE_APP);
        when(jobsService.pollAndWaitForJobFinished(TestConstants.TEST_JOB_GUID, Function.identity(), true)).thenReturn(jobStatus);
        when(jobsService.pollAndWaitForJobFinished(anyString(), any(Consumer.class), any(Consumer.class), any(Function.class), any(Supplier.class))).thenReturn(jobStatus.getState().toString());

        runStringArgs(fastScanCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_JOB_FAILED));
    }
}
