package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.AnalyzeCommand;
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

import java.util.Collections;
import java.util.Date;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {AipConsoleToolsCliIntegrationTest.class})
@ActiveProfiles(TestConstants.PROFILE_INTEGRATION_TEST)
public class AnalyzeCommandIntegrationTest extends AipConsoleToolsCliBaseTest {
    @Autowired
    private AnalyzeCommand analyzeCommand;

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
        resetSharedOptions(analyzeCommand.getSharedOptions());
        analyzeCommand.setApplicationName(null);
        analyzeCommand.setVersionName(null);
    }

    @Test
    public void testAnalyzeCommand_UsageHelp() {
        String expected = String.format("" +
                "Usage: Analysis [-hSV] [--process-imaging] [--verbose] [--apikey[=<apiKey>]]%n" +
                "                [--apikey:env=ENV_VAR_NAME] -n=APPLICATION_NAME%n" +
                "                [-s=AIP_CONSOLE_URL] [--timeout=<timeout>] [--user=<username>]%n" +
                "                [-v=VERSION_NAME]%n" +
                "Analyses an existing version on AIP Console%n" +
                "      --apikey[=<apiKey>]   The API Key to access AIP Console. Will prompt%n" +
                "                              entry if no value is passed.%n" +
                "      --apikey:env=ENV_VAR_NAME%n" +
                "                            The name of the environment variable containing the%n" +
                "                              AIP Key to access AIP Console%n" +
                "  -h, --help                Show this help message and exit.%n" +
                "  -n, --app-name=APPLICATION_NAME%n" +
                "                            The Name of the application to analyze%n" +
                "      --process-imaging     If provided, will upload data to Imaging. Note:%n" +
                "                              Parameter will be ignored if snapshot option is%n" +
                "                              not provided and Imaging is not setup in AIP%n" +
                "                              Console if specified without parameter: true%n" +
                "  -s, --server-url=AIP_CONSOLE_URL%n" +
                "                            The base URL for AIP Console (defaults to http:%n" +
                "                              //localhost:8081)%n" +
                "  -S, --snapshot            Creates a snapshot after running the analysis. if%n" +
                "                              specified without parameter: true%n" +
                "      --timeout=<timeout>   The timeout in seconds for calls to AIP Console.%n" +
                "                              Defaults to a 90s timeout%n" +
                "      --user=<username>     User name. Use this if no API Key generation is%n" +
                "                              available on AIP Console. Provide the user's%n" +
                "                              password in the apikey parameter.%n" +
                "  -v, --version-name=VERSION_NAME%n" +
                "                            The name of the version to analyze. If omitted, the%n" +
                "                              latest version will be used.%n" +
                "  -V, --version             Print version information and exit.%n" +
                "      --verbose             Whether the command log should be output to the%n" +
                "                              console or not, defaulted to true if specified%n" +
                "                              without parameter: true%n"
        );
        String actual = new CommandLine(analyzeCommand, springAwareCommandFactory)
                .getUsageMessage(CommandLine.Help.Ansi.OFF);
        assertThat(expected, is(actual));
    }

    @Test
    public void testAnalyzeCommand_UnexpectedParameters() {
        boolean verbose = true;
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--no-clone", "--auto-create", "--enable-security-dataflow", "--backup"
                , "-S"};

        runStringArgs(analyzeCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(getUnExpectedParameters().size(), greaterThan(1));
        assertThat(exitCode, is(1)); //no associated enum
    }

    @Test
    public void testAnalyzeCommand_WrongApplicationName() throws ApplicationServiceException {
        boolean verbose = true;
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "-S"};
        when(applicationService.getApplicationGuidFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(null);

        runStringArgs(analyzeCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_APPLICATION_NOT_FOUND));
    }

    @Test
    public void testAnalyzeCommand_ApplicationWithoutVersion() throws ApplicationServiceException {
        boolean verbose = true;
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "-S"};
        when(applicationService.getApplicationGuidFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(TestConstants.TEST_APP_GUID);
        //Set<VersionDto> versions =
        when(applicationService.getApplicationVersion(TestConstants.TEST_APP_GUID)).thenReturn(Collections.emptySet());

        runStringArgs(analyzeCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_APPLICATION_NO_VERSION));
    }

    @Test
    public void testAnalyzeCommand_WrongVersionNameToAnalyze() throws ApplicationServiceException {
        boolean verbose = true;
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "-S"};
        when(applicationService.getApplicationGuidFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(TestConstants.TEST_APP_GUID);
        VersionDto versionDto = new VersionDto();
        versionDto.setName("The-Current-Version");
        when(applicationService.getApplicationVersion(TestConstants.TEST_APP_GUID)).thenReturn(Sets.newSet(versionDto));

        runStringArgs(analyzeCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_APPLICATION_VERSION_NOT_FOUND));
    }

    @Test
    public void testAnalyzeCommand_WrongVersionStatusToAnalyze() throws ApplicationServiceException {
        boolean verbose = true;
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "-S"};
        when(applicationService.getApplicationGuidFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(TestConstants.TEST_APP_GUID);
        VersionDto versionDto = new VersionDto();
        versionDto.setName(TestConstants.TEST_VERSION_NAME);
        versionDto.setStatus(VersionStatus.DELIVERING);
        when(applicationService.getApplicationVersion(TestConstants.TEST_APP_GUID)).thenReturn(Sets.newSet(versionDto));

        runStringArgs(analyzeCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_APPLICATION_VERSION_NOT_FOUND));
    }

    @Test
    public void testAnalyzeCommand_JobCompleted() throws ApplicationServiceException, UploadException, JobServiceException, PackagePathInvalidException {
        boolean verbose = true;
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "-S", "--process-imaging"};

        when(applicationService.getApplicationGuidFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(TestConstants.TEST_APP_GUID);
        //Set<VersionDto> versions =
        VersionDto versionDto = new VersionDto();
        versionDto.setName(TestConstants.TEST_VERSION_NAME);
        versionDto.setStatus(VersionStatus.DELIVERED);
        when(applicationService.getApplicationVersion(TestConstants.TEST_APP_GUID)).thenReturn(Sets.newSet(versionDto));
        when(jobsService.startJob(any(JobRequestBuilder.class))).thenReturn(TestConstants.TEST_JOB_GUID);

        JobStatusWithSteps jobStatus = new JobStatusWithSteps();
        jobStatus.setAppGuid(TestConstants.TEST_APP_GUID);
        jobStatus.setState(JobState.COMPLETED);
        jobStatus.setCreated(new Date());
        jobStatus.setAppName(TestConstants.TEST_CREATRE_APP);
        when(jobsService.pollAndWaitForJobFinished(anyString(), any(Function.class), anyBoolean())).thenReturn(jobStatus);

        runStringArgs(analyzeCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }

}
