package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.SnapshotCommand;
import com.castsoftware.aip.console.tools.core.dto.VersionDto;
import com.castsoftware.aip.console.tools.core.dto.VersionStatus;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobRequestBuilder;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobStatusWithSteps;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.PackagePathInvalidException;
import com.castsoftware.aip.console.tools.core.exceptions.UploadException;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import picocli.CommandLine;

import java.util.Date;
import java.util.function.Function;

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
    protected void cleanupTestCommant() {
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
    public void testSnapshotCommand_UsageHelp() {
        String expected = String.format("" +
                "Usage: Snapshot [-hV] [--process-imaging] [--verbose] [--apikey[=<apiKey>]]%n" +
                "                [--apikey:env=ENV_VAR_NAME] -n=APPLICATION_NAME%n" +
                "                [-s=AIP_CONSOLE_URL] [-S=SNAPSHOT_NAME] [--timeout=<timeout>]%n" +
                "                [--user=<username>] [-v=VERSION_NAME]%n" +
                "Runs a snapshot on AIP Console%n" +
                "      --apikey[=<apiKey>]   The API Key to access AIP Console. Will prompt%n" +
                "                              entry if no value is passed.%n" +
                "      --apikey:env=ENV_VAR_NAME%n" +
                "                            The name of the environment variable containing the%n" +
                "                              AIP Key to access AIP Console%n" +
                "  -h, --help                Show this help message and exit.%n" +
                "  -n, --app-name=APPLICATION_NAME%n" +
                "                            The Name of the application to analyze%n" +
                "      --process-imaging     If provided, will upload data to Imaging if%n" +
                "                              specified without parameter: true%n" +
                "  -s, --server-url=AIP_CONSOLE_URL%n" +
                "                            The base URL for AIP Console (defaults to http:%n" +
                "                              //localhost:8081)%n" +
                "  -S, --snapshot-name=SNAPSHOT_NAME%n" +
                "                            The name of the snapshot to create%n" +
                "      --timeout=<timeout>   The timeout in seconds for calls to AIP Console.%n" +
                "                              Defaults to a 90s timeout%n" +
                "      --user=<username>     User name. Use this if no API Key generation is%n" +
                "                              available on AIP Console. Provide the user's%n" +
                "                              password in the apikey parameter.%n" +
                "  -v, --version-name=VERSION_NAME%n" +
                "                            The name of the version for which the snapshot will%n" +
                "                              be run%n" +
                "  -V, --version             Print version information and exit.%n" +
                "      --verbose             Whether the command log should be output to the%n" +
                "                              console or not, defaulted to true if specified%n" +
                "                              without parameter: true%n"
        );
        String actual = new CommandLine(snapshotCommand, springAwareCommandFactory)
                .getUsageMessage(CommandLine.Help.Ansi.OFF);
        assertThat(expected, is(actual));
    }

    @Test
    public void testSnapshotCommand_WhenAnalysisNotDone() throws ApplicationServiceException, UploadException, JobServiceException, PackagePathInvalidException {
        boolean verbose = true;
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--process-imaging"};

        when(applicationService.getApplicationGuidFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(TestConstants.TEST_APP_GUID);
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
    public void testSnapshotCommand_JobCompleted() throws ApplicationServiceException, JobServiceException {
        boolean verbose = true;
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--process-imaging"};

        when(applicationService.getApplicationGuidFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(TestConstants.TEST_APP_GUID);
        //Set<VersionDto> versions =
        VersionDto versionDto = new VersionDto();
        versionDto.setName(TestConstants.TEST_VERSION_NAME);
        versionDto.setStatus(VersionStatus.ANALYSIS_DONE);
        when(applicationService.getApplicationVersion(TestConstants.TEST_APP_GUID)).thenReturn(Sets.newSet(versionDto));
        when(jobsService.startJob(any(JobRequestBuilder.class))).thenReturn(TestConstants.TEST_JOB_GUID);

        JobStatusWithSteps jobStatus = new JobStatusWithSteps();
        jobStatus.setAppGuid(TestConstants.TEST_APP_GUID);
        jobStatus.setState(JobState.COMPLETED);
        jobStatus.setCreated(new Date());
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

        when(applicationService.getApplicationGuidFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(TestConstants.TEST_APP_GUID);
        //Set<VersionDto> versions =
        VersionDto versionDto = new VersionDto();
        versionDto.setName(TestConstants.TEST_VERSION_NAME);
        versionDto.setStatus(VersionStatus.ANALYSIS_DONE);
        when(applicationService.getApplicationVersion(TestConstants.TEST_APP_GUID)).thenReturn(Sets.newSet(versionDto));
        when(jobsService.startJob(any(JobRequestBuilder.class))).thenReturn(TestConstants.TEST_JOB_GUID);

        JobStatusWithSteps jobStatus = new JobStatusWithSteps();
        jobStatus.setAppGuid(TestConstants.TEST_APP_GUID);
        jobStatus.setState(JobState.STOPPED);
        jobStatus.setCreated(new Date());
        jobStatus.setAppName(TestConstants.TEST_CREATRE_APP);
        when(jobsService.pollAndWaitForJobFinished(anyString(), any(Function.class), anyBoolean())).thenReturn(jobStatus);

        runStringArgs(snapshotCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_JOB_FAILED));
    }
}
