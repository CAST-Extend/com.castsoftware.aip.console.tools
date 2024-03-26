package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.PublishToImagingCommand;
import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationOnboardingDto;
import com.castsoftware.aip.console.tools.core.dto.Applications;
import com.castsoftware.aip.console.tools.core.dto.ImagingSettingsDto;
import com.castsoftware.aip.console.tools.core.dto.VersionDto;
import com.castsoftware.aip.console.tools.core.dto.VersionStatus;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobExecutionDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.ScanAndReScanApplicationJobRequest;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.services.ApplicationServiceImpl;
import com.castsoftware.aip.console.tools.core.utils.ApiEndpointHelper;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.providers.CliLogPollingProviderImpl;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import picocli.CommandLine;

import java.io.IOException;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {AipConsoleToolsCliIntegrationTest.class})
@ActiveProfiles(TestConstants.PROFILE_INTEGRATION_TEST)
public class PublishToImagingCommandIntegrationTest extends AipConsoleToolsCliBaseTest {
    @InjectMocks
    private PublishToImagingCommand publishToImagingCommand;

    @InjectMocks
    private ApplicationServiceImpl applicationServiceImpl;
    ApplicationDto applicationDto;

    @Override
    protected void initializePrivateMocks() {
        assignMockedBeans(publishToImagingCommand);
    }

    @Override
    protected void additionalStartup() throws IOException {
        ReflectionTestUtils.setField(applicationServiceImpl, "restApiService", restApiService);
        ReflectionTestUtils.setField(applicationServiceImpl, "jobService", jobsService);
        ReflectionTestUtils.setField(applicationServiceImpl, "uploadService", uploadService);
        applicationDto = ApplicationDto.builder()
                .guid(TestConstants.TEST_APP_GUID)
                .name(TestConstants.TEST_CREATE_APP).build();
    }

    @Override
    protected void cleanupTestCommand() {
        resetSharedOptions(publishToImagingCommand.getSharedOptions());
        publishToImagingCommand.setApplicationName(null);
    }

    @Test
    public void testPublishToImagingCommand_invalidParameter() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATE_APP,
                "-f", zippedSourcesPath.toString()}; //not expected

        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion("2.5.2-funcrel").build();
        doReturn(apiInfoDto).when(restApiService).getAipConsoleApiInfo();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();
        doReturn(getTestApplicationMock()).when(applicationService).getApplicationFromName(TestConstants.TEST_CREATE_APP);
        initializeApplicationServiceMocks(getTestApplicationMock(), false);

        runStringArgs(publishToImagingCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_INVALID_PARAMETERS_ERROR));
    }

    @Test
    public void testPublishToImagingCommand_OnboardApplicationSettingsOFF() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATE_APP
        };

        doReturn(getTestApplicationMock()).when(applicationService).getApplicationFromName(TestConstants.TEST_CREATE_APP);
        doReturn(false).when(applicationService).isOnboardingSettingsEnabled();
        initializeApplicationServiceMocks(getTestApplicationMock(), false);

        runStringArgs(publishToImagingCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_ONBOARD_APPLICATION_DISABLED));
    }

    private void initializeApplicationServiceMocks(ApplicationDto applicationDto, boolean onboardFlag) throws ApiCallException {
        Applications applications = new Applications();
        applications.setApplications(applicationDto != null ? Sets.newHashSet(applicationDto) : Sets.newHashSet());
        when(restApiService.getForEntity(ApiEndpointHelper.getApplicationsPath(), Applications.class)).thenReturn(applications);
        when(restApiService.getForEntity(ApiEndpointHelper.getEnableOnboardingSettingsEndPoint(), Boolean.class)).thenReturn(onboardFlag);

        // The way to trigger real method call
        when(applicationService.publishToImaging(anyString(), anyLong(), anyBoolean(), any(CliLogPollingProviderImpl.class)))
                .then(i -> applicationServiceImpl.publishToImaging(i.getArgument(0), i.getArgument(1), i.getArgument(2), i.getArgument(3)));
    }

    @Test
    public void testPublishToImagingCommand_ApplicationNotExists() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATE_APP};

        doReturn(null).when(applicationService).getApplicationFromName(TestConstants.TEST_CREATE_APP);
        initializeApplicationServiceMocks(null, false);
        runStringArgs(publishToImagingCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_APPLICATION_NOT_FOUND));
    }

    @Test
    public void testPublishToImagingCommand() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATE_APP};

        mockPublishToImagingCommand(true, JobState.COMPLETED);
        initializeApplicationServiceMocks(applicationDto, true);

        runStringArgs(publishToImagingCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    @Test
    public void testPublishToImagingCommand_JobFailed() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATE_APP};

        mockPublishToImagingCommand(true, JobState.FAILED);
        initializeApplicationServiceMocks(applicationDto, true);

        runStringArgs(publishToImagingCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_JOB_FAILED));
    }

    @Test
    public void testPublishToImagingCommand_NonOnboardedApplication() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATE_APP};

        mockPublishToImagingCommand(false, JobState.COMPLETED);
        when(jobsService.startPublishToImaging(TestConstants.TEST_APP_GUID, null, null)).thenReturn(TestConstants.TEST_JOB_GUID);

        initializeApplicationServiceMocks(applicationDto, true);

        runStringArgs(publishToImagingCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    private void mockPublishToImagingCommand(boolean onboarded, JobState jobState) throws JobServiceException, ApiCallException, ApplicationServiceException {
        applicationDto.setOnboarded(onboarded);
        applicationDto.setSchemaPrefix("ShouldHave_One");

        VersionDto existingVersion = Mockito.mock(VersionDto.class);
        when(existingVersion.getGuid()).thenReturn(TestConstants.TEST_OBR_VERSION_GUID);
        when(existingVersion.getName()).thenReturn(TestConstants.TEST_OBR_VERSION_NAME);
        when(existingVersion.isImagingDone()).thenReturn(true);
        when(existingVersion.getVersionDate()).thenReturn(LocalDateTime.now());
        when(existingVersion.getStatus()).thenReturn(VersionStatus.FULLY_ANALYZED);
        applicationDto.setVersion(existingVersion);
        applicationDto.setImagingTenant("default"); //something different suit also
        doReturn(applicationDto).when(applicationService).getApplicationDetails(TestConstants.TEST_APP_GUID);
        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATE_APP)).thenReturn(applicationDto);

        ApplicationOnboardingDto onboardedAppDto = Mockito.mock(ApplicationOnboardingDto.class);
        when(onboardedAppDto.getCaipVersion()).thenReturn("8.3.45");
        when(applicationService.getApplicationOnboarding(TestConstants.TEST_APP_GUID)).thenReturn(onboardedAppDto);
        when(applicationService.isImagingAvailable()).thenReturn(true);

        ImagingSettingsDto imagingDto = Mockito.mock(ImagingSettingsDto.class);
        when(imagingDto.isValid()).thenReturn(true);
        when(restApiService.getForEntity(ApiEndpointHelper.getImagingSettingsEndPoint(), ImagingSettingsDto.class)).thenReturn(imagingDto);
        when(restApiService.getForEntity(ApiEndpointHelper.getApplicationPath(TestConstants.TEST_APP_GUID), ApplicationDto.class)).thenReturn(applicationDto);
        when(restApiService.getForEntity(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(Sets.newHashSet(existingVersion));
        when(jobsService.startDeepAnalysis(any(ScanAndReScanApplicationJobRequest.class))).thenReturn(TestConstants.TEST_JOB_GUID);

        JobExecutionDto jobStatus = new JobExecutionDto();
        jobStatus.setAppGuid(TestConstants.TEST_APP_GUID);
        jobStatus.setState(jobState);
        jobStatus.setCreatedDate(new Date());
        jobStatus.setAppName(TestConstants.TEST_CREATE_APP);
        when(jobsService.pollAndWaitForJobFinished(TestConstants.TEST_JOB_GUID, Function.identity(), true)).thenReturn(jobStatus);
        when(jobsService.pollAndWaitForJobFinished(anyString(), any(Consumer.class), any(Consumer.class), any(Function.class), any(Supplier.class))).thenReturn(jobStatus.getState().toString());
    }
}
