package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.BasicCallable;
import com.castsoftware.aip.console.tools.commands.TccCommands.TccCommand;
import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationCommonDetailsDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobExecutionDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.tcc.ComputeFunctionPointsProperties;
import com.castsoftware.aip.console.tools.core.services.ApplicationServiceImpl;
import com.castsoftware.aip.console.tools.core.utils.ApiEndpointHelper;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {AipConsoleToolsCliIntegrationTest.class})
@ActiveProfiles(TestConstants.PROFILE_INTEGRATION_TEST)
public class TccCommandTest extends AipConsoleToolsCliBaseTest {
    @InjectMocks
    private TccCommand tccCommand;
    @Spy
    @InjectMocks
    private ApplicationServiceImpl applicationServiceImpl;

    @Override
    protected void initializePrivateMocks() {
        assignMockedBeans(tccCommand);
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
        resetSharedOptions(tccCommand.getSharedOptions());
        tccCommand.setApplicationName(null);
    }

    @Override
    protected void assignMockedBeans(BasicCallable command) {
        super.assignMockedBeans(command);

        Class<? extends BasicCallable> commandClass = command.getClass();
        Field springAwareCommandField = ReflectionUtils.findField(commandClass, "springAwareCommandFactory");
        if (springAwareCommandField != null) {
            ReflectionUtils.makeAccessible(springAwareCommandField);
            ReflectionUtils.setField(springAwareCommandField, command, springAwareCommandFactory);
        }
    }

    ApplicationCommonDetailsDto applicationCommonDetailsDto;

    @Override
    protected void additionalStartup() throws IOException {
        super.additionalStartup();

        ReflectionTestUtils.setField(applicationServiceImpl, "restApiService", restApiService);
        ReflectionTestUtils.setField(applicationServiceImpl, "jobService", jobsService);

        applicationCommonDetailsDto = new ApplicationCommonDetailsDto();
        applicationCommonDetailsDto.setName(TestConstants.TEST_CREATE_APP);
        applicationCommonDetailsDto.setGuid(TestConstants.TEST_APP_GUID);
    }

    @Test
    public void testTccCommand_WithNotCompatibleVersion() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name=" + TestConstants.TEST_CREATE_APP};
        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion("2.4.9-funcrel").build();
        doReturn(apiInfoDto).when(restApiService).getAipConsoleApiInfo();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();
        when(restApiService.getForEntity("/api/", ApiInfoDto.class)).thenReturn(apiInfoDto);

        runStringArgs(tccCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_BAD_SERVER_VERSION));
    }

    @Test
    public void testTccCommand_WithCompatibleVersion() throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATE_APP};

        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion("3.0.0").build();
        doReturn(apiInfoDto).when(restApiService).getAipConsoleApiInfo();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();
        when(restApiService.getForEntity("/api/", ApiInfoDto.class)).thenReturn(apiInfoDto);

        runStringArgs(tccCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    @Test
    public void testTccComputeFunctionPoints_AppNotPresent() throws Exception {
        String[] args = new String[]{"compute-function-points", "--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATE_APP};

        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doReturn(null).when(applicationServiceImpl).getApplicationDetailsFromName(TestConstants.TEST_CREATE_APP);
        when(applicationService.computeFunctionPoints(any(ComputeFunctionPointsProperties.class))).then(i -> applicationServiceImpl.computeFunctionPoints(i.getArgument(0)));

        runStringArgs(tccCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_APPLICATION_NOT_FOUND));
    }

    @Test
    public void testTccComputeFunctionPoints_AppNotManaged() throws Exception {
        String[] args = new String[]{"compute-function-points", "--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATE_APP};

        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doReturn(applicationCommonDetailsDto).when(applicationServiceImpl).getApplicationDetailsFromName(TestConstants.TEST_CREATE_APP);
        ApplicationDto applicationDto = ApplicationDto.builder().managed(false).build();
        doReturn(applicationDto).when(applicationServiceImpl).getApplicationDetails(TestConstants.TEST_APP_GUID);
        when(applicationService.computeFunctionPoints(any(ComputeFunctionPointsProperties.class))).then(i -> applicationServiceImpl.computeFunctionPoints(i.getArgument(0)));

        runStringArgs(tccCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_APPLICATION_INFO_MISSING));
    }

    @Test
    public void testTccComputeFunctionPoints_JobSuccess() throws Exception {
        String[] args = new String[]{"compute-function-points", "--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATE_APP};

        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doReturn(applicationCommonDetailsDto).when(applicationServiceImpl).getApplicationDetailsFromName(TestConstants.TEST_CREATE_APP);

        ApplicationDto applicationDto = ApplicationDto.builder().targetNode("testNode1").managed(true).build();
        doReturn(applicationDto).when(applicationServiceImpl).getApplicationDetails(TestConstants.TEST_APP_GUID);
        when(applicationService.computeFunctionPoints(any(ComputeFunctionPointsProperties.class))).then(i -> applicationServiceImpl.computeFunctionPoints(i.getArgument(0)));

        when(jobsService.startComputeFunctionPoints(TestConstants.TEST_APP_GUID, "testNode1")).thenReturn(TestConstants.TEST_JOB_GUID);

        JobExecutionDto jobStatus = new JobExecutionDto();
        jobStatus.setAppGuid(TestConstants.TEST_APP_GUID);
        jobStatus.setAppName(TestConstants.TEST_CREATE_APP);
        jobStatus.setState(JobState.COMPLETED);
        when(jobsService.pollAndWaitForJobFinished(eq(TestConstants.TEST_JOB_GUID), any(Consumer.class), any(Consumer.class), any(Function.class), any(Supplier.class))).thenReturn(jobStatus.getState().toString());

        runStringArgs(tccCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    @Test
    public void testTccComputeFunctionPoints_JobFailed() throws Exception {
        String[] args = new String[]{"compute-function-points", "--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATE_APP};

        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doReturn(applicationCommonDetailsDto).when(applicationServiceImpl).getApplicationDetailsFromName(TestConstants.TEST_CREATE_APP);

        ApplicationDto applicationDto = ApplicationDto.builder().targetNode("testNode1").managed(true).build();
        doReturn(applicationDto).when(applicationServiceImpl).getApplicationDetails(TestConstants.TEST_APP_GUID);
        when(applicationService.computeFunctionPoints(any(ComputeFunctionPointsProperties.class))).then(i -> applicationServiceImpl.computeFunctionPoints(i.getArgument(0)));

        when(jobsService.startComputeFunctionPoints(TestConstants.TEST_APP_GUID, "testNode1")).thenReturn(TestConstants.TEST_JOB_GUID);

        JobExecutionDto jobStatus = new JobExecutionDto();
        jobStatus.setAppGuid(TestConstants.TEST_APP_GUID);
        jobStatus.setAppName(TestConstants.TEST_CREATE_APP);
        jobStatus.setState(JobState.FAILED);
        when(jobsService.pollAndWaitForJobFinished(eq(TestConstants.TEST_JOB_GUID), any(Consumer.class), any(Consumer.class), any(Function.class), any(Supplier.class))).thenReturn(jobStatus.getState().toString());

        runStringArgs(tccCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_JOB_FAILED));
    }

    @Test
    public void testTccComputeTransactions_AppNotPresent() throws Exception {
        String[] args = new String[]{"compute-function-points", "--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATE_APP};

        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doReturn(null).when(applicationServiceImpl).getApplicationDetailsFromName(TestConstants.TEST_CREATE_APP);
        when(applicationService.computeFunctionPoints(any(ComputeFunctionPointsProperties.class))).then(i -> applicationServiceImpl.computeFunctionPoints(i.getArgument(0)));

        runStringArgs(tccCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_APPLICATION_NOT_FOUND));
    }

    @Test
    public void testTccComputeTransactions_AppNotManaged() throws Exception {
        String[] args = new String[]{"compute-function-points", "--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATE_APP};

        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doReturn(applicationCommonDetailsDto).when(applicationServiceImpl).getApplicationDetailsFromName(TestConstants.TEST_CREATE_APP);
        ApplicationDto applicationDto = ApplicationDto.builder().managed(false).build();
        doReturn(applicationDto).when(applicationServiceImpl).getApplicationDetails(TestConstants.TEST_APP_GUID);
        when(applicationService.computeFunctionPoints(any(ComputeFunctionPointsProperties.class))).then(i -> applicationServiceImpl.computeFunctionPoints(i.getArgument(0)));

        runStringArgs(tccCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_APPLICATION_INFO_MISSING));
    }

    @Test
    public void testTccComputeTransactions_JobSuccess() throws Exception {
        String[] args = new String[]{"compute-function-points", "--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATE_APP};

        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doReturn(applicationCommonDetailsDto).when(applicationServiceImpl).getApplicationDetailsFromName(TestConstants.TEST_CREATE_APP);

        ApplicationDto applicationDto = ApplicationDto.builder().targetNode("testNode1").managed(true).build();
        doReturn(applicationDto).when(applicationServiceImpl).getApplicationDetails(TestConstants.TEST_APP_GUID);
        when(applicationService.computeFunctionPoints(any(ComputeFunctionPointsProperties.class))).then(i -> applicationServiceImpl.computeFunctionPoints(i.getArgument(0)));

        when(jobsService.startComputeFunctionPoints(TestConstants.TEST_APP_GUID, "testNode1")).thenReturn(TestConstants.TEST_JOB_GUID);

        JobExecutionDto jobStatus = new JobExecutionDto();
        jobStatus.setAppGuid(TestConstants.TEST_APP_GUID);
        jobStatus.setAppName(TestConstants.TEST_CREATE_APP);
        jobStatus.setState(JobState.COMPLETED);
        when(jobsService.pollAndWaitForJobFinished(eq(TestConstants.TEST_JOB_GUID), any(Consumer.class), any(Consumer.class), any(Function.class), any(Supplier.class))).thenReturn(jobStatus.getState().toString());

        runStringArgs(tccCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    @Test
    public void testTccComputeTransactions_JobFailed() throws Exception {
        String[] args = new String[]{"compute-function-points", "--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATE_APP};

        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());
        doReturn(applicationCommonDetailsDto).when(applicationServiceImpl).getApplicationDetailsFromName(TestConstants.TEST_CREATE_APP);

        ApplicationDto applicationDto = ApplicationDto.builder().targetNode("testNode1").managed(true).build();
        doReturn(applicationDto).when(applicationServiceImpl).getApplicationDetails(TestConstants.TEST_APP_GUID);
        when(applicationService.computeFunctionPoints(any(ComputeFunctionPointsProperties.class))).then(i -> applicationServiceImpl.computeFunctionPoints(i.getArgument(0)));

        when(jobsService.startComputeFunctionPoints(TestConstants.TEST_APP_GUID, "testNode1")).thenReturn(TestConstants.TEST_JOB_GUID);

        JobExecutionDto jobStatus = new JobExecutionDto();
        jobStatus.setAppGuid(TestConstants.TEST_APP_GUID);
        jobStatus.setAppName(TestConstants.TEST_CREATE_APP);
        jobStatus.setState(JobState.FAILED);
        when(jobsService.pollAndWaitForJobFinished(eq(TestConstants.TEST_JOB_GUID), any(Consumer.class), any(Consumer.class), any(Function.class), any(Supplier.class))).thenReturn(jobStatus.getState().toString());

        runStringArgs(tccCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_JOB_FAILED));
    }

    @Test
    public void testTccUpdateFunctionPointSettings() throws Exception {
        String[] invalidFormatNewSettings = new String[]{"update-settings", "--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATE_APP, "--new-settings",
                "key=value|key1=value"};

        String[] invalidSettingKeys = new String[]{"update-settings", "--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATE_APP, "--new-settings",
                "invalidKey=1,DF_DEFAULT_TYPE=ILF"};

        String[] invalidSettingValues = new String[]{"update-settings", "--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATE_APP, "--new-settings",
                "TF_ESTIMATED_FP_VALUE=1,DF_DEFAULT_TYPE=ILF"};

        String[] validArgs = new String[]{"update-settings", "--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATE_APP, "--new-settings",
                "TF_ESTIMATED_FP_VALUE=ASSESS,DF_DEFAULT_TYPE=EIF"};

        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());

        // invalid --new-settings argument format
        runStringArgs(tccCommand, invalidFormatNewSettings);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_INVALID_PARAMETERS_ERROR));

        // invalid setting keys
        runStringArgs(tccCommand, invalidSettingKeys);
        spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_INVALID_PARAMETERS_ERROR));

        // invalid setting values
        runStringArgs(tccCommand, invalidSettingValues);
        spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_INVALID_PARAMETERS_ERROR));

        doReturn(applicationCommonDetailsDto).when(applicationServiceImpl).getApplicationDetailsFromName(TestConstants.TEST_CREATE_APP);
        ApplicationDto applicationDto = ApplicationDto.builder().targetNode("testNode1").managed(true).build();
        doReturn(applicationDto).when(applicationServiceImpl).getApplicationDetails(TestConstants.TEST_APP_GUID);
        when(restApiService.putForEntity(ApiEndpointHelper.getUpdateFunctionPointSettingEndPoint(eq(TestConstants.TEST_APP_GUID), anyString(), anyString()), null, String.class)).thenReturn("");
        when(applicationService.updateFunctionPointSettings(eq(TestConstants.TEST_CREATE_APP), any(Map.class))).then(i -> applicationServiceImpl.updateFunctionPointSettings(i.getArgument(0), i.getArgument(1)));

        // update settings with valid setting keys and values
        runStringArgs(tccCommand, validArgs);
        spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }
}
