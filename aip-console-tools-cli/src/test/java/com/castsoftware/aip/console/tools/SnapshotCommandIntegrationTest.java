package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.SnapshotCommand;
import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.VersionDto;
import com.castsoftware.aip.console.tools.core.dto.VersionStatus;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobExecutionDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobRequestBuilder;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.ScanAndReScanApplicationJobRequest;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.providers.CliLogPollingProviderImpl;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import picocli.CommandLine;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {AipConsoleToolsCliIntegrationTest.class})
@ActiveProfiles(TestConstants.PROFILE_INTEGRATION_TEST)
public class SnapshotCommandIntegrationTest extends AipConsoleToolsCliBaseTest {
    @Autowired
    private SnapshotCommand snapshotCommand;

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
        resetSharedOptions(snapshotCommand.getSharedOptions());
        snapshotCommand.setApplicationName(null);
        snapshotCommand.setVersionName(null);
        snapshotCommand.setSnapshotName(null);
    }

    @Test
    public void testSnapshotCommand_WhenAnalysisNotDone() throws ApplicationServiceException {
        boolean verbose = true;
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--process-imaging"};

        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(TestConstants.TEST_APP);
        VersionDto versionDto = new VersionDto();
        versionDto.setName(TestConstants.TEST_VERSION_NAME);
        versionDto.setStatus(VersionStatus.ACCEPTED);
        when(applicationService.getApplicationVersion(TestConstants.TEST_APP_GUID)).thenReturn(Sets.newSet(versionDto));
        runStringArgs(snapshotCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_VERSION_WITH_ANALYSIS_DONE_NOT_FOUND));
    }

    @Test
    public void testSnapshotCommand_WhenFastScanWorkFlow_JobFailed() throws ApplicationServiceException {
        boolean verbose = true;
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--snapshot-name", "SNAP-Name-here",
                "--process-imaging"};

        ApplicationDto applicationDto = ApplicationDto.builder()
                .guid(TestConstants.TEST_APP_GUID)
                .name(TestConstants.TEST_CREATRE_APP)
                .onboarded(true)
                .schemaPrefix("ShouldHave_One").build();

        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(applicationDto);
        VersionDto versionDto = new VersionDto();
        versionDto.setName(TestConstants.TEST_VERSION_NAME);
        versionDto.setStatus(VersionStatus.ANALYSIS_DONE);
        when(applicationService.getApplicationVersion(TestConstants.TEST_APP_GUID)).thenReturn(Sets.newSet(versionDto));

        runStringArgs(snapshotCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_JOB_FAILED));
    }

    @Test
    public void testSnapshotCommand_WhenFastScanWorkFlow_JobSucceeded() throws ApplicationServiceException {
        boolean verbose = true;
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--snapshot-name", "SNAP-Name-here",
                "--process-imaging"};

        ApplicationDto applicationDto = ApplicationDto.builder()
                .guid(TestConstants.TEST_APP_GUID)
                .name(TestConstants.TEST_CREATRE_APP)
                .onboarded(true)
                .schemaPrefix("ShouldHave_One").build();

        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(applicationDto);
        VersionDto versionDto = new VersionDto();
        versionDto.setName(TestConstants.TEST_VERSION_NAME);
        versionDto.setStatus(VersionStatus.ANALYSIS_DONE);
        when(applicationService.getApplicationVersion(TestConstants.TEST_APP_GUID)).thenReturn(Sets.newSet(versionDto));
        when(applicationService.runDeepAnalysis(any(ScanAndReScanApplicationJobRequest.class), any(CliLogPollingProviderImpl.class))).thenReturn(TestConstants.TEST_APP_GUID);

        runStringArgs(snapshotCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    @Test
    public void testSnapshotCommand_JobCompleted() throws ApplicationServiceException, JobServiceException {
        boolean verbose = true;
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--process-imaging"};

        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(TestConstants.TEST_APP);
        //Set<VersionDto> versions =
        VersionDto versionDto = new VersionDto();
        versionDto.setName(TestConstants.TEST_VERSION_NAME);
        versionDto.setStatus(VersionStatus.ANALYSIS_DONE);
        when(applicationService.getApplicationVersion(TestConstants.TEST_APP_GUID)).thenReturn(Sets.newSet(versionDto));
        when(jobsService.startJob(any(JobRequestBuilder.class))).thenReturn(TestConstants.TEST_JOB_GUID);

        JobExecutionDto jobStatus = new JobExecutionDto();
        jobStatus.setAppGuid(TestConstants.TEST_APP_GUID);
        jobStatus.setState(JobState.COMPLETED);
        jobStatus.setCreatedDate(new Date());
        jobStatus.setAppName(TestConstants.TEST_CREATRE_APP);
        when(jobsService.pollAndWaitForJobFinished(anyString(), any(Function.class), anyBoolean())).thenReturn(jobStatus);

        runStringArgs(snapshotCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    @Test
    public void testSnapshotCommand_JobFailed() throws ApplicationServiceException, JobServiceException {
        boolean verbose = true;
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--process-imaging"};

        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(TestConstants.TEST_APP);
        //Set<VersionDto> versions =
        VersionDto versionDto = new VersionDto();
        versionDto.setName(TestConstants.TEST_VERSION_NAME);
        versionDto.setStatus(VersionStatus.ANALYSIS_DONE);
        when(applicationService.getApplicationVersion(TestConstants.TEST_APP_GUID)).thenReturn(Sets.newSet(versionDto));
        when(jobsService.startJob(any(JobRequestBuilder.class))).thenReturn(TestConstants.TEST_JOB_GUID);

        JobExecutionDto jobStatus = new JobExecutionDto();
        jobStatus.setAppGuid(TestConstants.TEST_APP_GUID);
        jobStatus.setState(JobState.STOPPED);
        jobStatus.setCreatedDate(new Date());
        jobStatus.setAppName(TestConstants.TEST_CREATRE_APP);
        when(jobsService.pollAndWaitForJobFinished(anyString(), any(Function.class), anyBoolean())).thenReturn(jobStatus);

        runStringArgs(snapshotCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_JOB_FAILED));
    }

    private void prepareForJobStatus(JobState state) throws ApplicationServiceException, JobServiceException {
        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(TestConstants.TEST_APP);

        VersionDto versionDto = new VersionDto();
        versionDto.setName(TestConstants.TEST_VERSION_NAME);
        versionDto.setStatus(VersionStatus.ANALYSIS_DONE);
        when(applicationService.getApplicationVersion(TestConstants.TEST_APP_GUID)).thenReturn(Sets.newSet(versionDto));
        when(jobsService.startJob(any(JobRequestBuilder.class))).thenReturn(TestConstants.TEST_JOB_GUID);

        JobExecutionDto jobStatus = new JobExecutionDto();
        jobStatus.setAppGuid(TestConstants.TEST_APP_GUID);
        jobStatus.setState(state);
        jobStatus.setCreatedDate(new Date());
        jobStatus.setAppName(TestConstants.TEST_CREATRE_APP);
        when(jobsService.pollAndWaitForJobFinished(anyString(), any(Function.class), anyBoolean())).thenReturn(jobStatus);
    }

    @Test
    public void testSnapshotCommand_WithConsolidation() throws ApplicationServiceException, JobServiceException {
        boolean verbose = true;
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--process-imaging=false", "--upload-application"};

        prepareForJobStatus(JobState.COMPLETED);
        runStringArgs(snapshotCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));

        List<CommandLine.Model.ArgSpec> argsSpec = spec.args();
        Map<String, Boolean> consolidateTags = argsSpec.stream().filter(a -> StringUtils.equalsAny(a.paramLabel(), ARG_CONSOLIDATE_LABEL, ARG_IMAGING_LABEL))
                .collect(Collectors.toMap(CommandLine.Model.ArgSpec::paramLabel, this::getBooleanArgValue));

        boolean consolidationArgValue = consolidateTags.get(ARG_CONSOLIDATE_LABEL);
        assertThat(consolidationArgValue, is(true));

        boolean forcedConsolidation = consolidateTags.get(ARG_IMAGING_LABEL) || consolidationArgValue;
        assertThat(spec, is(notNullValue()));
        assertThat(forcedConsolidation, is(true));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    @Test
    public void testSnapshotCommand_WithoutConsolidation() throws ApplicationServiceException, JobServiceException {
        boolean verbose = true;
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--process-imaging=false", "--consolidation=false"};

        prepareForJobStatus(JobState.COMPLETED);
        runStringArgs(snapshotCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));

        List<CommandLine.Model.ArgSpec> argsSpec = spec.args();
        Map<String, Boolean> consolidateTags = argsSpec.stream().filter(a -> StringUtils.equalsAny(a.paramLabel(), ARG_CONSOLIDATE_LABEL, ARG_IMAGING_LABEL))
                .collect(Collectors.toMap(CommandLine.Model.ArgSpec::paramLabel, this::getBooleanArgValue));

        boolean consolidationArgValue = consolidateTags.get(ARG_CONSOLIDATE_LABEL);
        assertThat(consolidationArgValue, is(false));
        assertThat(consolidateTags.get(ARG_IMAGING_LABEL), is(false));

        boolean forcedConsolidation = consolidateTags.get(ARG_IMAGING_LABEL) || consolidationArgValue;
        assertThat(spec, is(notNullValue()));
        assertThat(forcedConsolidation, is(false));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    @Test
    public void testSnapshotCommand_WithJustImaging() throws ApplicationServiceException, JobServiceException {
        boolean verbose = true;
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--process-imaging", "--consolidation=false"};

        prepareForJobStatus(JobState.COMPLETED);
        runStringArgs(snapshotCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));

        List<CommandLine.Model.ArgSpec> argsSpec = spec.args();
        Map<String, Boolean> consolidateTags = argsSpec.stream().filter(a -> StringUtils.equalsAny(a.paramLabel(), ARG_CONSOLIDATE_LABEL, ARG_IMAGING_LABEL))
                .collect(Collectors.toMap(CommandLine.Model.ArgSpec::paramLabel, this::getBooleanArgValue));

        boolean consolidationArgValue = consolidateTags.get(ARG_CONSOLIDATE_LABEL);
        assertThat(consolidationArgValue, is(false));
        assertThat(consolidateTags.get(ARG_IMAGING_LABEL), is(true));

        boolean forcedConsolidation = consolidateTags.get(ARG_IMAGING_LABEL) || consolidationArgValue;
        assertThat(spec, is(notNullValue()));
        assertThat(forcedConsolidation, is(true));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }
}
