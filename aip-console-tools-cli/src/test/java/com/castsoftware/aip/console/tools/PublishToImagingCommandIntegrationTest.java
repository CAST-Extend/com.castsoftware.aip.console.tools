package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.PublishToImagingCommand;
import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.Applications;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.services.ApplicationServiceImpl;
import com.castsoftware.aip.console.tools.core.utils.ApiEndpointHelper;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.providers.CliLogPollingProviderImpl;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import picocli.CommandLine;

import java.io.IOException;

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

    @Override
    protected void initializePrivateMocks() {
        assignMockedBeans(publishToImagingCommand);
    }

    @Override
    protected void additionalStartup() throws IOException {
        ReflectionTestUtils.setField(applicationServiceImpl, "restApiService", restApiService);
        ReflectionTestUtils.setField(applicationServiceImpl, "jobService", jobsService);
        ReflectionTestUtils.setField(applicationServiceImpl, "uploadService", uploadService);
    }

    @Override
    protected void cleanupTestCommand() {
        resetSharedOptions(publishToImagingCommand.getSharedOptions());
        publishToImagingCommand.setApplicationName(null);
    }

    @Test
    public void testPublishToImagingCommand_invalidParameter() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATRE_APP,
                "-f", zippedSourcesPath.toString()}; //not expected

        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion("2.5.2-funcrel").build();
        doReturn(apiInfoDto).when(restApiService).getAipConsoleApiInfo();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();
        doReturn(getTestApplicationMock()).when(applicationService).getApplicationFromName(TestConstants.TEST_CREATRE_APP);
        initializeApplicationServiceMocks(getTestApplicationMock());

        runStringArgs(publishToImagingCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_INVALID_PARAMETERS_ERROR));
    }

    @Test
    public void testPublishToImagingCommand_OnboardApplicationSettingsOFF() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATRE_APP
        };

        doReturn(getTestApplicationMock()).when(applicationService).getApplicationFromName(TestConstants.TEST_CREATRE_APP);
        doReturn(false).when(applicationService).isOnboardingSettingsEnabled();
        initializeApplicationServiceMocks(getTestApplicationMock());

        runStringArgs(publishToImagingCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_ONBOARD_APPLICATION_DISABLED));
    }

    private void initializeApplicationServiceMocks(ApplicationDto applicationDto) throws ApiCallException {
        Applications applications = new Applications();
        applications.setApplications(applicationDto != null ? Sets.newHashSet(applicationDto) : Sets.newHashSet());
        when(restApiService.getForEntity(ApiEndpointHelper.getApplicationsPath(), Applications.class)).thenReturn(applications);
        when(restApiService.getForEntity(ApiEndpointHelper.getEnableOnboardingSettingsEndPoint(), Boolean.class)).thenReturn(false);

        boolean verbose = false;
        long duration = 10;
        // The way to trigger real method call
        when(applicationService.publishToImaging(anyString(), anyLong(), anyBoolean(), any(CliLogPollingProviderImpl.class)))
                .then(i -> applicationServiceImpl.publishToImaging(i.getArgument(0), i.getArgument(1), i.getArgument(2), i.getArgument(3)));
    }

    @Test
    public void testPublishToImagingCommand_ApplicationNotExists() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATRE_APP};

        doReturn(null).when(applicationService).getApplicationFromName(TestConstants.TEST_CREATRE_APP);
        initializeApplicationServiceMocks(null);
        runStringArgs(publishToImagingCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_APPLICATION_NOT_FOUND));
    }
}
