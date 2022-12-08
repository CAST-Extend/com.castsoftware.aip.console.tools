package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.PublishToImagingCommand;
import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import picocli.CommandLine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.doReturn;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {AipConsoleToolsCliIntegrationTest.class})
@ActiveProfiles(TestConstants.PROFILE_INTEGRATION_TEST)
public class PublishToImagingCommandIntegrationTest extends AipConsoleToolsCliBaseTest {
    @InjectMocks
    private PublishToImagingCommand publishToImagingCommand;

    @Override
    protected void initializePrivateMocks() {
        assignMockedBeans(publishToImagingCommand);
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

        runStringArgs(publishToImagingCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_INVALID_PARAMETERS_ERROR));
    }

    @Test
    public void testPublishToImagingCommand_OnboarApplicationSettingsOFF() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATRE_APP
        };

        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion("2.5.1-funcrel").build();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();
        doReturn(getTestApplicationMock()).when(applicationService).getApplicationFromName(TestConstants.TEST_CREATRE_APP);
        doReturn(false).when(applicationService).isOnboardingSettingsEnabled();

        runStringArgs(publishToImagingCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_ONBOARD_APPLICATION_DISABLED));
    }

    @Test
    public void testPublishToImagingCommand_ApplicationNotExists() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATRE_APP};

        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion("2.5.1-funcrel").build();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();
        doReturn(null).when(applicationService).getApplicationFromName(TestConstants.TEST_CREATRE_APP);

        runStringArgs(publishToImagingCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_APPLICATION_NOT_FOUND));
    }
}
