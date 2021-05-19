package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.AddVersionCommand;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import picocli.CommandLine;

import java.io.File;
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
public class AddVersionCommandIntegrationTest extends AipConsoleToolsCliBaseTest {
    @Autowired
    private AddVersionCommand addVersionCommand;

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
        resetSharedOptions(addVersionCommand.getSharedOptions());
        addVersionCommand.setApplicationGuid(null);
        addVersionCommand.setApplicationName(null);
        addVersionCommand.setBackupName(null);
        addVersionCommand.setDomainName(null);
        addVersionCommand.setFilePath(null);
        addVersionCommand.setNodeName(null);
        addVersionCommand.setVersionName(null);
    }

    @Test
    public void testUsageHelp() {
        String expected = String.format("" + "Usage: AddVersion [-bhV] [--auto-create] [--enable-security-dataflow]%n" +
                "                  [--no-clone] [--process-imaging] [--verbose] [--apikey%n" +
                "                  [=<apiKey>]] [-a=APPLICATION_GUID] [--apikey:%n" +
                "                  env=ENV_VAR_NAME] [--backup-name=BACKUP_NAME]%n" +
                "                  [--domain-name=DOMAIN_NAME] -f=FILE [-n=APPLICATION_NAME]%n" +
                "                  [--node-name=NODE_NAME] [-s=AIP_CONSOLE_URL]%n" +
                "                  [--snapshot-name=SNAPSHOT_NAME] [--timeout=<timeout>]%n" +
                "                  [--user=<username>] [-v=VERSION_NAME]%n" +
                "Creates a new version, runs an analysis and creates a snapshot for an%n" +
                "application on AIP Console%n" +
                "  -a, --app-guid=APPLICATION_GUID%n" +
                "                            The GUID of the application to rescan%n" +
                "      --apikey[=<apiKey>]   The API Key to access AIP Console. Will prompt%n" +
                "                              entry if no value is passed.%n" +
                "      --apikey:env=ENV_VAR_NAME%n" +
                "                            The name of the environment variable containing the%n" +
                "                              AIP Key to access AIP Console%n" +
                "      --auto-create         If the given application name doesn't exist on the%n" +
                "                              target server, it'll be automatically created%n" +
                "                              before creating a new version if specified%n" +
                "                              without parameter: true%n" +
                "  -b, --backup              Enable backup of application before delivering the%n" +
                "                              new version if specified without parameter: true%n" +
                "      --backup-name=BACKUP_NAME%n" +
                "                            The name of the backup to create before delivering%n" +
                "                              the new version. Defaults to 'backup_date.time'%n" +
                "      --domain-name=DOMAIN_NAME%n" +
                "                            The name of the domain to assign to the%n" +
                "                              application. Will be created if it doesn't%n" +
                "                              exists. No domain will be assigned if left empty.%n" +
                "      --enable-security-dataflow%n" +
                "                            If defined, this will activate the security%n" +
                "                              dataflow for this version if specified without%n" +
                "                              parameter: true%n" +
                "  -f, --file=FILE           A local zip or tar.gz file OR a path to a folder on%n" +
                "                              the node where the source if saved%n" +
                "  -h, --help                Show this help message and exit.%n" +
                "  -n, --app-name=APPLICATION_NAME%n" +
                "                            The Name of the application to rescan%n" +
                "      --no-clone, --no-rescan, --new-configuration%n" +
                "                            Enable this flag to create a new version without%n" +
                "                              cloning the latest version configuration. if%n" +
                "                              specified without parameter: true%n" +
                "      --node-name=NODE_NAME The name of the node on which the application will%n" +
                "                              be created. Ignored if no --auto-create or the%n" +
                "                              application already exists.%n" +
                "      --process-imaging     If provided, will upload data to Imaging if%n" +
                "                              specified without parameter: true%n" +
                "  -s, --server-url=AIP_CONSOLE_URL%n" +
                "                            The base URL for AIP Console (defaults to http:%n" +
                "                              //localhost:8081)%n" +
                "      --snapshot-name=SNAPSHOT_NAME%n" +
                "                            The name of the snapshot to generate%n" +
                "      --timeout=<timeout>   The timeout in seconds for calls to AIP Console.%n" +
                "                              Defaults to a 90s timeout%n" +
                "      --user=<username>     User name. Use this if no API Key generation is%n" +
                "                              available on AIP Console. Provide the user's%n" +
                "                              password in the apikey parameter.%n" +
                "  -v, --version-name=VERSION_NAME%n" +
                "                            The name of the version to create%n" +
                "  -V, --version             Print version information and exit.%n" +
                "      --verbose             Whether the command log should be output to the%n" +
                "                              console or not, defaulted to true if specified%n" +
                "                              without parameter: true%n");
        String actual = new CommandLine(addVersionCommand, springAwareCommandFactory)
                .getUsageMessage(CommandLine.Help.Ansi.OFF);
        assertThat(expected, is(actual));
    }

    @Test
    public void testAddVersionCommand_FailToCreateApplication() throws ApplicationServiceException {
        String[] args = defaultArgs;
        // No existing application
        when(applicationService.getOrCreateApplicationFromName(anyString(), anyBoolean(), anyString(), anyString(), anyBoolean())).thenReturn(null);
        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(AipConsoleToolsCliBaseTest.simplifiedModeApp);

        runStringArgs(addVersionCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_APPLICATION_NOT_FOUND));
    }

    @Test
    public void testAddVersionCommand_SimplifiedDeliveryWithFileProvided() throws ApplicationServiceException {
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY, "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "-f", zippedSourcesPath.toString(),
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--node-name", TestConstants.TEST_NODE};

        // gives the existing application
        when(applicationService.getOrCreateApplicationFromName(any(String.class), anyBoolean(), any(String.class), any(String.class), anyBoolean())).thenReturn(TestConstants.TEST_APP_GUID);
        when(applicationService.getApplicationNameFromGuid(TestConstants.TEST_APP_GUID)).thenReturn(TestConstants.TEST_CREATRE_APP);
        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(AipConsoleToolsCliBaseTest.simplifiedModeApp);

        runStringArgs(addVersionCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_INPLACE_MODE_ERROR));
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
        when(applicationService.getOrCreateApplicationFromName(any(String.class), anyBoolean(), any(String.class), any(String.class), anyBoolean())).thenReturn(TestConstants.TEST_APP_GUID);
        when(applicationService.getApplicationNameFromGuid(TestConstants.TEST_APP_GUID)).thenReturn(TestConstants.TEST_CREATRE_APP);
        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(AipConsoleToolsCliBaseTest.simplifiedModeApp);
        when(uploadService.uploadFileAndGetSourcePath(any(String.class), any(String.class), any(File.class))).thenReturn(sflPath.toString());
        when(applicationService.applicationHasVersion(TestConstants.TEST_APP_GUID)).thenReturn(false);
        when(applicationService.createDeliveryConfiguration(TestConstants.TEST_APP_GUID, sflPath.toString(), null, false)).thenReturn(TestConstants.TEST_DELIVERY_CONFIG_GUID);
        when(jobsService.startAddVersionJob(any(JobRequestBuilder.class))).thenReturn(TestConstants.TEST_JOB_GUID);

        JobStatusWithSteps jobStatus = new JobStatusWithSteps();
        jobStatus.setAppGuid(TestConstants.TEST_APP_GUID);
        jobStatus.setState(JobState.COMPLETED);
        jobStatus.setCreated(new Date());
        jobStatus.setAppName(TestConstants.TEST_CREATRE_APP);
        when(jobsService.pollAndWaitForJobFinished(TestConstants.TEST_JOB_GUID, Function.identity(), true)).thenReturn(jobStatus);

        runStringArgs(addVersionCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    @Test
    public void testAddVersionCommand() throws ApplicationServiceException {
        String[] args = defaultArgs;

        // gives the existing application
        when(applicationService.getOrCreateApplicationFromName(anyString(), anyBoolean(), anyString(), anyString(), anyBoolean()))
                .thenReturn(TestConstants.TEST_APP_GUID);
        when(applicationService.getApplicationNameFromGuid(TestConstants.TEST_APP_GUID)).thenReturn(TestConstants.TEST_CREATRE_APP);

        runStringArgs(addVersionCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
    }


}
