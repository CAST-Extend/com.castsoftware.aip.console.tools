package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.AddVersionCommand;
import com.castsoftware.aip.console.tools.core.dto.DebugOptionsDto;
import com.castsoftware.aip.console.tools.core.dto.Exclusions;
import com.castsoftware.aip.console.tools.core.dto.jobs.CreateJobsRequest;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobExecutionDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobRequestBuilder;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.PackagePathInvalidException;
import com.castsoftware.aip.console.tools.core.exceptions.UploadException;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import picocli.CommandLine;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {AipConsoleToolsCliIntegrationTest.class})
@ActiveProfiles(TestConstants.PROFILE_INTEGRATION_TEST)
public class AddVersionCommandIntegrationTest extends AipConsoleToolsCliBaseTest {
    @Autowired
    private AddVersionCommand addVersionCommand;

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
        resetSharedOptions(addVersionCommand.getSharedOptions());
        addVersionCommand.setApplicationGuid(null);
        addVersionCommand.setApplicationName(null);
        addVersionCommand.setBackupName(null);
        addVersionCommand.setDomainName(null);
        addVersionCommand.setFilePath(null);
        addVersionCommand.setNodeName(null);
        addVersionCommand.setVersionName(null);
        addVersionCommand.setCssServerName(null);
    }

    @Test
    public void testAddVersionCommand_FailToCreateApplication() throws ApplicationServiceException {
        String[] args = defaultArgs;
        // No existing application
        when(applicationService.getOrCreateApplicationFromName(anyString(), anyBoolean(), anyString(), anyString(), eq(null), anyBoolean())).thenReturn(null);
        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(AipConsoleToolsCliBaseTest.simplifiedModeApp);

        runStringArgs(addVersionCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_APPLICATION_NOT_FOUND));
    }

    @Test
    public void testAddVersionCommand_SimplifiedDeliveryWithFileProvided() throws ApplicationServiceException, JobServiceException, UploadException, PackagePathInvalidException {
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY, "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "-f", zippedSourcesPath.toString(),
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--node-name", TestConstants.TEST_NODE};

        // gives the existing application
        prepareJobExecution();
        createJobStatus(JobState.COMPLETED);

        runStringArgs(addVersionCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    @Test
    public void testAddVersionCommand_RunAddVersionJobCompleted() throws ApplicationServiceException, UploadException, JobServiceException, PackagePathInvalidException {
        boolean verbose = true;
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY, "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "-f", sflPath.toString(),
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--node-name", TestConstants.TEST_NODE};

        // gives the existing application
        prepareJobExecution();
        createJobStatus(JobState.COMPLETED);

        runStringArgs(addVersionCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    @Test
    public void testAddVersionCommand_RunAddVersionJobCanceled() throws ApplicationServiceException, UploadException, JobServiceException, PackagePathInvalidException {
        boolean verbose = true;
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY, "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "-f", sflPath.toString(),
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--node-name", TestConstants.TEST_NODE};

        // gives the existing application
        prepareJobExecution();
        createJobStatus(JobState.CANCELED);

        runStringArgs(addVersionCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_JOB_CANCELED));
    }

    private void prepareJobExecution() throws ApplicationServiceException, JobServiceException, UploadException, PackagePathInvalidException {
        when(applicationService.getOrCreateApplicationFromName(any(String.class), anyBoolean(), any(String.class), any(String.class), eq(null), anyBoolean())).thenReturn(TestConstants.TEST_APP_GUID);
        when(applicationService.getApplicationFromGuid(TestConstants.TEST_APP_GUID)).thenReturn(AipConsoleToolsCliBaseTest.simplifiedModeApp);
        when(uploadService.uploadFileAndGetSourcePath(any(String.class), any(String.class), any(File.class))).thenReturn(sflPath.toString());
        when(applicationService.applicationHasVersion(TestConstants.TEST_APP_GUID)).thenReturn(false);
        when(applicationService.createDeliveryConfiguration(TestConstants.TEST_APP_GUID, sflPath.toString(), Exclusions.builder().build(), false)).thenReturn(TestConstants.TEST_DELIVERY_CONFIG_GUID);
        when(jobsService.startAddVersionJob(any(JobRequestBuilder.class))).thenReturn(TestConstants.TEST_JOB_GUID);
        DebugOptionsDto debugOptions = Mockito.mock(DebugOptionsDto.class);
        when(debugOptions.isActivateAmtMemoryProfile()).thenReturn(false);
        when(applicationService.getDebugOptions(TestConstants.TEST_APP_GUID)).thenReturn(debugOptions);
    }

    private JobExecutionDto createJobStatus(JobState status) throws JobServiceException {
        JobExecutionDto jobStatus = new JobExecutionDto();
        jobStatus.setAppGuid(TestConstants.TEST_APP_GUID);
        jobStatus.setState(status);
        jobStatus.setCreatedDate(new Date());
        jobStatus.setAppName(TestConstants.TEST_CREATRE_APP);
        when(jobsService.pollAndWaitForJobFinished(TestConstants.TEST_JOB_GUID, Function.identity(), true)).thenReturn(jobStatus);
        return jobStatus;
    }

    @Test
    public void testAddVersionCommand() throws ApplicationServiceException {
        String[] args = defaultArgs;

        // gives the existing application
        when(applicationService.getOrCreateApplicationFromName(anyString(), anyBoolean(), anyString(), anyString(), eq(null), anyBoolean()))
                .thenReturn(TestConstants.TEST_APP_GUID);
        when(applicationService.getApplicationFromGuid(TestConstants.TEST_APP_GUID)).thenReturn(AipConsoleToolsCliBaseTest.simplifiedModeApp);

        runStringArgs(addVersionCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
    }

    @Test
    public void testAddVersionCommand_NoConsolidationWithoutImaging() throws ApplicationServiceException, JobServiceException, UploadException, PackagePathInvalidException {
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY, "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "-f", zippedSourcesPath.toString(),
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--node-name", TestConstants.TEST_NODE
                , "--process-imaging=false", "--snapshot-name", "SNAP-NAME", "--upload-application=false"
        };

        // gives the existing application
        when(applicationService.getOrCreateApplicationFromName(any(String.class), anyBoolean(), any(String.class), any(String.class), eq(null), anyBoolean())).thenReturn(TestConstants.TEST_APP_GUID);
        when(applicationService.getApplicationFromGuid(TestConstants.TEST_APP_GUID)).thenReturn(AipConsoleToolsCliBaseTest.simplifiedModeApp);

        when(uploadService.uploadFileAndGetSourcePath(any(String.class), any(String.class), any(File.class))).thenReturn(sflPath.toString());
        when(applicationService.applicationHasVersion(TestConstants.TEST_APP_GUID)).thenReturn(false);
        when(applicationService.createDeliveryConfiguration(TestConstants.TEST_APP_GUID, sflPath.toString(), Exclusions.builder().build(), false)).thenReturn(TestConstants.TEST_DELIVERY_CONFIG_GUID);
        when(jobsService.startAddVersionJob(any(JobRequestBuilder.class))).thenReturn(TestConstants.TEST_JOB_GUID);
        DebugOptionsDto debugOptions = Mockito.mock(DebugOptionsDto.class);
        when(debugOptions.isActivateAmtMemoryProfile()).thenReturn(false);
        when(applicationService.getDebugOptions(TestConstants.TEST_APP_GUID)).thenReturn(debugOptions);

        JobExecutionDto jobStatus = new JobExecutionDto();
        jobStatus.setAppGuid(TestConstants.TEST_APP_GUID);
        jobStatus.setState(JobState.COMPLETED);
        jobStatus.setCreatedDate(new Date());
        jobStatus.setAppName(TestConstants.TEST_CREATRE_APP);
        when(jobsService.pollAndWaitForJobFinished(TestConstants.TEST_JOB_GUID, Function.identity(), true)).thenReturn(jobStatus);

        runStringArgs(addVersionCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        List<CommandLine.Model.ArgSpec> argsSpec = spec.args();
        Map<String, Boolean> consolidateTags = argsSpec.stream().filter(a -> StringUtils.equalsAny(a.paramLabel(), AipConsoleToolsCliBaseTest.ARG_CONSOLIDATE_LABEL, AipConsoleToolsCliBaseTest.ARG_IMAGING_LABEL))
                .collect(Collectors.toMap(CommandLine.Model.ArgSpec::paramLabel, this::getBooleanArgValue));

        boolean consolidationArgValue = consolidateTags.get(AipConsoleToolsCliBaseTest.ARG_CONSOLIDATE_LABEL);
        assertThat(consolidationArgValue, is(false));

        boolean forcedConsolidation = consolidateTags.get(AipConsoleToolsCliBaseTest.ARG_IMAGING_LABEL) || consolidationArgValue;
        assertThat(spec, is(notNullValue()));
        assertThat(forcedConsolidation, is(false));

        assertNotNull(addVersionCommand.getBuilder());
        CreateJobsRequest jobRequest = addVersionCommand.getBuilder().buildJobRequest();
        String endStep = (String) jobRequest.getJobParameters().get(Constants.PARAM_END_STEP);
        assertEquals("snapshot_indicator", endStep);

        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    @Test
    public void testAddVersionCommand_DefaultConsolidationWithoutImaging() throws ApplicationServiceException, JobServiceException, UploadException, PackagePathInvalidException {
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY, "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "-f", zippedSourcesPath.toString(),
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--node-name", TestConstants.TEST_NODE
                , "--process-imaging=false"
        };

        // gives the existing application
        when(applicationService.getOrCreateApplicationFromName(any(String.class), anyBoolean(), any(String.class), any(String.class), eq(null), anyBoolean())).thenReturn(TestConstants.TEST_APP_GUID);
        when(applicationService.getApplicationFromGuid(TestConstants.TEST_APP_GUID)).thenReturn(AipConsoleToolsCliBaseTest.simplifiedModeApp);

        when(uploadService.uploadFileAndGetSourcePath(any(String.class), any(String.class), any(File.class))).thenReturn(sflPath.toString());
        when(applicationService.applicationHasVersion(TestConstants.TEST_APP_GUID)).thenReturn(false);
        when(applicationService.createDeliveryConfiguration(TestConstants.TEST_APP_GUID, sflPath.toString(), Exclusions.builder().build(), false)).thenReturn(TestConstants.TEST_DELIVERY_CONFIG_GUID);
        when(jobsService.startAddVersionJob(any(JobRequestBuilder.class))).thenReturn(TestConstants.TEST_JOB_GUID);
        DebugOptionsDto debugOptions = Mockito.mock(DebugOptionsDto.class);
        when(debugOptions.isActivateAmtMemoryProfile()).thenReturn(false);
        when(applicationService.getDebugOptions(TestConstants.TEST_APP_GUID)).thenReturn(debugOptions);

        JobExecutionDto jobStatus = new JobExecutionDto();
        jobStatus.setAppGuid(TestConstants.TEST_APP_GUID);
        jobStatus.setState(JobState.COMPLETED);
        jobStatus.setCreatedDate(new Date());
        jobStatus.setAppName(TestConstants.TEST_CREATRE_APP);
        when(jobsService.pollAndWaitForJobFinished(TestConstants.TEST_JOB_GUID, Function.identity(), true)).thenReturn(jobStatus);

        runStringArgs(addVersionCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        List<CommandLine.Model.ArgSpec> argsSpec = spec.args();
        Map<String, Boolean> consolidateTags = argsSpec.stream().filter(a -> StringUtils.equalsAny(a.paramLabel(), AipConsoleToolsCliBaseTest.ARG_CONSOLIDATE_LABEL, AipConsoleToolsCliBaseTest.ARG_IMAGING_LABEL))
                .collect(Collectors.toMap(CommandLine.Model.ArgSpec::paramLabel, this::getBooleanArgValue));

        boolean consolidationArgValue = consolidateTags.get(AipConsoleToolsCliBaseTest.ARG_CONSOLIDATE_LABEL);
        assertThat(consolidationArgValue, is(true));

        boolean forcedConsolidation = consolidateTags.get(AipConsoleToolsCliBaseTest.ARG_IMAGING_LABEL) || consolidationArgValue;
        assertThat(spec, is(notNullValue()));
        assertThat(forcedConsolidation, is(true));

        assertNotNull(addVersionCommand.getBuilder());
        CreateJobsRequest jobRequest = addVersionCommand.getBuilder().buildJobRequest();
        String endStep = (String) jobRequest.getJobParameters().get(Constants.PARAM_END_STEP);
        assertEquals("consolidate_snapshot", endStep); //so that Console will move up to publish to health

        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    @Test
    public void testAddVersionCommand_DefaultConsolidationWithImaging() throws ApplicationServiceException, JobServiceException, UploadException, PackagePathInvalidException {
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY, "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "-f", zippedSourcesPath.toString(),
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--node-name", TestConstants.TEST_NODE
                , "--process-imaging"
        };

        // gives the existing application
        when(applicationService.getOrCreateApplicationFromName(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyBoolean())).thenReturn(null);
        when(applicationService.getApplicationNameFromGuid(TestConstants.TEST_APP_GUID)).thenReturn(TestConstants.TEST_CREATRE_APP);
        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(AipConsoleToolsCliBaseTest.simplifiedModeApp);

        when(uploadService.uploadFileAndGetSourcePath(any(String.class), any(String.class), any(File.class))).thenReturn(sflPath.toString());
        when(applicationService.applicationHasVersion(TestConstants.TEST_APP_GUID)).thenReturn(false);
        when(applicationService.createDeliveryConfiguration(TestConstants.TEST_APP_GUID, sflPath.toString(), null, false)).thenReturn(TestConstants.TEST_DELIVERY_CONFIG_GUID);
        when(jobsService.startAddVersionJob(any(JobRequestBuilder.class))).thenReturn(TestConstants.TEST_JOB_GUID);

        JobExecutionDto jobStatus = new JobExecutionDto();
        jobStatus.setAppGuid(TestConstants.TEST_APP_GUID);
        jobStatus.setState(JobState.COMPLETED);
        jobStatus.setCreatedDate(new Date());
        jobStatus.setAppName(TestConstants.TEST_CREATRE_APP);
        when(jobsService.pollAndWaitForJobFinished(TestConstants.TEST_JOB_GUID, Function.identity(), true)).thenReturn(jobStatus);

        runStringArgs(addVersionCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();

        String ARG_CONSOLIDATE_LABEL = "<consolidation>";
        String ARG_IMAGING_LABEL = "<processImaging>";

        List<CommandLine.Model.ArgSpec> argsSpec = spec.args();
        Map<String, Boolean> consolidateTags = argsSpec.stream().filter(a -> StringUtils.equalsAny(a.paramLabel(), ARG_CONSOLIDATE_LABEL, ARG_IMAGING_LABEL))
                .collect(Collectors.toMap(CommandLine.Model.ArgSpec::paramLabel, this::getBooleanArgValue));

        boolean consolidationArgValue = consolidateTags.get(ARG_CONSOLIDATE_LABEL);
        assertThat(consolidationArgValue, is(true));

        boolean forcedConsolidation = consolidateTags.get(ARG_IMAGING_LABEL) || consolidationArgValue;
        assertThat(spec, is(notNullValue()));
        assertThat(forcedConsolidation, is(true));
        assertThat(consolidateTags.get(ARG_IMAGING_LABEL), is(true));

        assertNotNull(addVersionCommand.getBuilder());
        CreateJobsRequest jobRequest = addVersionCommand.getBuilder().buildJobRequest();
        String endStep = (String) jobRequest.getJobParameters().get(Constants.PARAM_END_STEP);
        assertEquals(Constants.PROCESS_IMAGING, endStep); //so that Console will move up to publish to health

        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    @Test
    public void testAddVersionCommand_InvalidModuleType() throws ApplicationServiceException {
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY, "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "-f", zippedSourcesPath.toString(),
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--module-option", "Invalid",
                "--node-name", TestConstants.TEST_NODE};
        // No existing application
        when(applicationService.getOrCreateApplicationFromName(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyBoolean())).thenReturn(null);
        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(AipConsoleToolsCliBaseTest.simplifiedModeApp);

        runStringArgs(addVersionCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        List<CommandLine.Model.ArgSpec> argsSpec = spec.args();
        assertThat(exitCode, is(Constants.RETURN_INVALID_PARAMETERS_ERROR));
    }

    @Test
    public void testAddVersionCommand_WithVersionDate() throws ApplicationServiceException, JobServiceException, UploadException, PackagePathInvalidException {
        String versionDateString = "2022-07-11T07:22:46";
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY, "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "-f", zippedSourcesPath.toString(),
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--node-name", TestConstants.TEST_NODE
                , "--process-imaging=false", "--snapshot-name", "SNAP-NAME"
                , "-date", versionDateString
        };

        // gives the existing application
        when(applicationService.getOrCreateApplicationFromName(any(String.class), anyBoolean(), any(String.class), any(String.class), eq(null), anyBoolean())).thenReturn(TestConstants.TEST_APP_GUID);
        when(applicationService.getApplicationFromGuid(TestConstants.TEST_APP_GUID)).thenReturn(AipConsoleToolsCliBaseTest.simplifiedModeApp);

        when(uploadService.uploadFileAndGetSourcePath(any(String.class), any(String.class), any(File.class))).thenReturn(sflPath.toString());
        when(applicationService.applicationHasVersion(TestConstants.TEST_APP_GUID)).thenReturn(false);
        when(applicationService.createDeliveryConfiguration(TestConstants.TEST_APP_GUID, sflPath.toString(), Exclusions.builder().build(), false)).thenReturn(TestConstants.TEST_DELIVERY_CONFIG_GUID);
        when(jobsService.startAddVersionJob(any(JobRequestBuilder.class))).thenReturn(TestConstants.TEST_JOB_GUID);
        DebugOptionsDto debugOptions = Mockito.mock(DebugOptionsDto.class);
        when(debugOptions.isActivateAmtMemoryProfile()).thenReturn(false);
        when(applicationService.getDebugOptions(TestConstants.TEST_APP_GUID)).thenReturn(debugOptions);

        JobExecutionDto jobStatus = new JobExecutionDto();
        jobStatus.setAppGuid(TestConstants.TEST_APP_GUID);
        jobStatus.setState(JobState.COMPLETED);
        jobStatus.setCreatedDate(new Date());
        jobStatus.setAppName(TestConstants.TEST_CREATRE_APP);
        when(applicationService.getVersionDate(versionDateString)).thenReturn(new Date(1657516966000L));
        when(jobsService.pollAndWaitForJobFinished(TestConstants.TEST_JOB_GUID, Function.identity(), true)).thenReturn(jobStatus);

        runStringArgs(addVersionCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();

        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }
}
