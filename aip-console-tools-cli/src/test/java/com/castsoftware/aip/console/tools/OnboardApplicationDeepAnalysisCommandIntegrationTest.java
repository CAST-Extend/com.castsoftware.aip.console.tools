package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.BasicCollable;
import com.castsoftware.aip.console.tools.commands.OnboardApplicationDeepAnalysisCommand;
import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationOnboardingDto;
import com.castsoftware.aip.console.tools.core.dto.VersionDto;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {AipConsoleToolsCliIntegrationTest.class})
@ActiveProfiles(TestConstants.PROFILE_INTEGRATION_TEST)
public class OnboardApplicationDeepAnalysisCommandIntegrationTest extends AipConsoleToolsCliBaseTest {
    @InjectMocks
    private OnboardApplicationDeepAnalysisCommand deepAnalysisCommand;

    @Override
    protected void initializePrivateMocks() {
        assignMockedBeans(deepAnalysisCommand);
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
        resetSharedOptions(deepAnalysisCommand.getSharedOptions());
        deepAnalysisCommand.setApplicationName(null);
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

    ApplicationDto applicationDto;

    private static final String CONSOLE_API_VERSION = "2.8.0-SNAPSHOT-133";
    @Override
    protected void additionalStartup() throws IOException {
        super.additionalStartup();
        applicationDto = ApplicationDto.builder()
                .guid(TestConstants.TEST_APP_GUID)
                .name(TestConstants.TEST_CREATRE_APP).build();
    }

    @Test
    public void testOnboardApplicationDeepAnalysis() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATRE_APP};

        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion(CONSOLE_API_VERSION).build();
        doReturn(apiInfoDto).when(restApiService).getAipConsoleApiInfo();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();
        when(restApiService.getForEntity("/api/", ApiInfoDto.class)).thenReturn(apiInfoDto);
        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doReturn(true).when(applicationService).isOnboardingSettingsEnabled();

        //first scan/ Refresh sources content done
        applicationDto.setOnboarded(true);
        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(applicationDto);
        VersionDto existingVersion = Mockito.mock(VersionDto.class);
        when(existingVersion.getGuid()).thenReturn(TestConstants.TEST_OBR_VERSION_GUID);
        when(existingVersion.getName()).thenReturn(TestConstants.TEST_OBR_VERSION_NAME);
        when(existingVersion.isImagingDone()).thenReturn(true);
        applicationDto.setVersion(existingVersion);
        applicationDto.setImagingTenant("default"); //something different suit also
        doReturn(applicationDto).when(applicationService).getApplicationDetails(TestConstants.TEST_APP_GUID);

        ApplicationOnboardingDto onboardedAppDto = Mockito.mock(ApplicationOnboardingDto.class);
        when(onboardedAppDto.getCaipVersion()).thenReturn("8.3.45");
        when(applicationService.getApplicationOnboarding(TestConstants.TEST_APP_GUID)).thenReturn(onboardedAppDto);
        when(applicationService.isImagingAvailable()).thenReturn(true);

        runStringArgs(deepAnalysisCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    @Test
    public void testOnboardApplicationDeepAnalysis_WithoutFirstScan() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATRE_APP};

        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion(CONSOLE_API_VERSION).build();
        doReturn(apiInfoDto).when(restApiService).getAipConsoleApiInfo();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();
        when(restApiService.getForEntity("/api/", ApiInfoDto.class)).thenReturn(apiInfoDto);
        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doReturn(true).when(applicationService).isOnboardingSettingsEnabled();

        //first scan/ Refresh sources content not done
        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(applicationDto);
        doReturn(applicationDto).when(applicationService).getApplicationDetails(TestConstants.TEST_APP_GUID);

        ApplicationOnboardingDto onboardedAppDto = Mockito.mock(ApplicationOnboardingDto.class);
        when(onboardedAppDto.getCaipVersion()).thenReturn("8.3.45");
        when(applicationService.getApplicationOnboarding(TestConstants.TEST_APP_GUID)).thenReturn(onboardedAppDto);
        when(applicationService.isImagingAvailable()).thenReturn(true);

        runStringArgs(deepAnalysisCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_ONBOARD_FAST_SCAN_REQUIRED));
    }
}
