package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.BasicCollable;
import com.castsoftware.aip.console.tools.commands.OnboardApplicationDeepAnalysisCommand;
import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationOnboardingDto;
import com.castsoftware.aip.console.tools.core.dto.Applications;
import com.castsoftware.aip.console.tools.core.dto.DeepAnalyzeProperties;
import com.castsoftware.aip.console.tools.core.dto.ImagingSettingsDto;
import com.castsoftware.aip.console.tools.core.dto.ModuleGenerationType;
import com.castsoftware.aip.console.tools.core.dto.VersionDto;
import com.castsoftware.aip.console.tools.core.services.ApplicationServiceImpl;
import com.castsoftware.aip.console.tools.core.utils.ApiEndpointHelper;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
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
    @InjectMocks
    private ApplicationServiceImpl applicationServiceImpl;

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

    @Override
    protected void additionalStartup() throws IOException {
        super.additionalStartup();

        ReflectionTestUtils.setField(applicationServiceImpl, "restApiService", restApiService);
        ReflectionTestUtils.setField(applicationServiceImpl, "jobService", jobsService);
        ReflectionTestUtils.setField(applicationServiceImpl, "uploadService", uploadService);

        applicationDto = ApplicationDto.builder()
                .guid(TestConstants.TEST_APP_GUID)
                .name(TestConstants.TEST_CREATRE_APP).build();
    }

    @Test
    public void testOnboardApplicationDeepAnalysis() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATRE_APP,
                "--module-option", "ONE_PER_AU"
        };

        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doReturn(true).when(applicationService).isOnboardingSettingsEnabled();

        //first scan/ Refresh sources content done
        applicationDto.setOnboarded(true);
        applicationDto.setSchemaPrefix("ShouldHave_One");
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

        Applications applications = new Applications();
        applications.setApplications(Sets.newHashSet(applicationDto));
        when(restApiService.getForEntity(ApiEndpointHelper.getApplicationsPath(), Applications.class)).thenReturn(applications);

        when(restApiService.getForEntity(ApiEndpointHelper.getEnableOnboardingSettingsEndPoint(), Boolean.class)).thenReturn(true);
        ImagingSettingsDto imagingDto = Mockito.mock(ImagingSettingsDto.class);
        when(imagingDto.isValid()).thenReturn(true);
        when(restApiService.getForEntity(ApiEndpointHelper.getImagingSettingsEndPoint(), ImagingSettingsDto.class)).thenReturn(imagingDto);
        when(restApiService.getForEntity(ApiEndpointHelper.getApplicationPath(TestConstants.TEST_APP_GUID), ApplicationDto.class)).thenReturn(applicationDto);
        when(applicationService.deepAnalyze(any(DeepAnalyzeProperties.class))).then(i -> applicationServiceImpl.deepAnalyze(i.getArgument(0)));

        runStringArgs(deepAnalysisCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));

        //Checks that the initial value set for the module type is full content
        List<CommandLine.Model.ArgSpec> argsSpec = spec.args();
        for (CommandLine.Model.ArgSpec cmdArg : argsSpec) {
            CommandLine.Model.OptionSpec optionSpec = (CommandLine.Model.OptionSpec) cmdArg;
            if (StringUtils.equalsAny("--module-option", optionSpec.names())) {
                assertThat(optionSpec.hasInitialValue(), is(true));
                assertThat(optionSpec.initialValue(), is(ModuleGenerationType.FULL_CONTENT));
                break;
            }
        }
        // Now check the argument passed to the CLI has been taken into account
        assertThat(deepAnalysisCommand.getModuleGenerationType(), is(ModuleGenerationType.ONE_PER_AU));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    @Test
    public void testOnboardApplicationDeepAnalysis_WithoutFastScan_AppDoesNotExist() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATRE_APP};

        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doReturn(true).when(applicationService).isOnboardingSettingsEnabled();

        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(null);
        doReturn(null).when(applicationService).getApplicationDetails(TestConstants.TEST_APP_GUID);
        when(applicationService.isImagingAvailable()).thenReturn(true);

        Applications applications = new Applications();
        applications.setApplications(Sets.newHashSet());
        when(restApiService.getForEntity(ApiEndpointHelper.getApplicationsPath(), Applications.class)).thenReturn(applications);

        when(restApiService.getForEntity(ApiEndpointHelper.getEnableOnboardingSettingsEndPoint(), Boolean.class)).thenReturn(true);
        ImagingSettingsDto imagingDto = Mockito.mock(ImagingSettingsDto.class);
        when(imagingDto.isValid()).thenReturn(true);
        when(restApiService.getForEntity(ApiEndpointHelper.getImagingSettingsEndPoint(), ImagingSettingsDto.class)).thenReturn(imagingDto);
        when(applicationService.deepAnalyze(any(DeepAnalyzeProperties.class))).then(i -> applicationServiceImpl.deepAnalyze(i.getArgument(0)));

        runStringArgs(deepAnalysisCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_ONBOARD_FAST_SCAN_REQUIRED));
    }

    @Test
    public void testOnboardApplicationDeepAnalysis_WithoutFastScan_ExistingAppNotUsingFastScanWorkflow() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATRE_APP};

        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doReturn(true).when(applicationService).isOnboardingSettingsEnabled();

        applicationDto.setName(TestConstants.TEST_CREATRE_APP);
        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(applicationDto);
        doReturn(applicationDto).when(applicationService).getApplicationDetails(TestConstants.TEST_APP_GUID);

        ApplicationOnboardingDto onboardedAppDto = Mockito.mock(ApplicationOnboardingDto.class);
        when(onboardedAppDto.getCaipVersion()).thenReturn("8.3.45");
        when(applicationService.getApplicationOnboarding(TestConstants.TEST_APP_GUID)).thenReturn(onboardedAppDto);
        when(applicationService.isImagingAvailable()).thenReturn(true);

        Applications applications = new Applications();
        applications.setApplications(Sets.newHashSet(applicationDto));
        when(restApiService.getForEntity(ApiEndpointHelper.getApplicationsPath(), Applications.class)).thenReturn(applications);
        when(restApiService.getForEntity(ApiEndpointHelper.getEnableOnboardingSettingsEndPoint(), Boolean.class)).thenReturn(true);
        ImagingSettingsDto imagingDto = Mockito.mock(ImagingSettingsDto.class);
        when(imagingDto.isValid()).thenReturn(true);
        when(restApiService.getForEntity(ApiEndpointHelper.getImagingSettingsEndPoint(), ImagingSettingsDto.class)).thenReturn(imagingDto);
        when(restApiService.getForEntity(ApiEndpointHelper.getApplicationPath(TestConstants.TEST_APP_GUID), ApplicationDto.class)).thenReturn(applicationDto);
        when(applicationService.deepAnalyze(any(DeepAnalyzeProperties.class))).then(i -> applicationServiceImpl.deepAnalyze(i.getArgument(0)));

        runStringArgs(deepAnalysisCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_ONBOARD_DEEP_ANALYSIS_FORBIDDEN));
    }
}
