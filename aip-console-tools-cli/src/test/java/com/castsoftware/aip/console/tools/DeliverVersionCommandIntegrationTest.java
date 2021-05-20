package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.DeliverVersionCommand;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {AipConsoleToolsCliIntegrationTest.class})
@ActiveProfiles(TestConstants.PROFILE_INTEGRATION_TEST)
public class DeliverVersionCommandIntegrationTest extends AipConsoleToolsCliBaseTest {
    @Autowired
    private DeliverVersionCommand deliverVersionCommand;

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
        resetSharedOptions(deliverVersionCommand.getSharedOptions());
        deliverVersionCommand.setApplicationName(null);
        deliverVersionCommand.setBackupName(null);
        deliverVersionCommand.setDomainName(null);
        deliverVersionCommand.setFilePath(null);
        deliverVersionCommand.setNodeName(null);
        deliverVersionCommand.setVersionName(null);
    }

    @Test
    public void testDeliverVersionCommand_UsageHelp() {
        String expected = String.format("" +
                "Usage: Deliver [-bhV] [--auto-create] [--auto-discover] [-current]%n" +
                "               [--enable-security-dataflow] [--no-clone] [--verbose] [--apikey%n" +
                "               [=<apiKey>]] [--apikey:env=ENV_VAR_NAME]%n" +
                "               [--backup-name=BACKUP_NAME] [--domain-name=DOMAIN_NAME]%n" +
                "               [-exclude=<exclusionPatterns>] -f=FILE -n=APPLICATION_NAME%n" +
                "               [--node-name=NODE_NAME] [-s=AIP_CONSOLE_URL]%n" +
                "               [--timeout=<timeout>] [--user=<username>] [-v=VERSION_NAME]%n" +
                "Delivers a new version to AIP Console%n" +
                "      --apikey[=<apiKey>]   The API Key to access AIP Console. Will prompt%n" +
                "                              entry if no value is passed.%n" +
                "      --apikey:env=ENV_VAR_NAME%n" +
                "                            The name of the environment variable containing the%n" +
                "                              AIP Key to access AIP Console%n" +
                "      --auto-create         If the given application name doesn't exist on the%n" +
                "                              target server, it'll be automatically created%n" +
                "                              before creating a new version if specified%n" +
                "                              without parameter: true%n" +
                "      --auto-discover       AIP Console will discover new technologies and%n" +
                "                              install new extensions, to disable if run%n" +
                "                              consistency check if specified without parameter:%n" +
                "                              true%n" +
                "  -b, --backup              Enable backup of application before delivering the%n" +
                "                              new version if specified without parameter: true%n" +
                "      --backup-name=BACKUP_NAME%n" +
                "                            The name of the backup to create before delivering%n" +
                "                              the new version. Defaults to 'backup_date.time'%n" +
                "      -current, --set-as-current%n" +
                "                            true or false depending on whether the version%n" +
                "                              should be set as the current one or not. if%n" +
                "                              specified without parameter: true%n" +
                "      --domain-name=DOMAIN_NAME%n" +
                "                            The name of the domain to assign to the%n" +
                "                              application. Will be created if it doesn't%n" +
                "                              exists. No domain will be assigned if left empty.%n" +
                "                              Will only be used when creating the application.%n" +
                "      --enable-security-dataflow%n" +
                "                            If defined, this will activate the security%n" +
                "                              dataflow for this version if specified without%n" +
                "                              parameter: true%n" +
                "      -exclude, --exclude-patterns=<exclusionPatterns>%n" +
                "                            File patterns(glob pattern) to exclude in the%n" +
                "                              delivery, separated with comma%n" +
                "  -f, --file=FILE           A local zip or tar.gz file OR a path to a folder on%n" +
                "                              the node where the source if saved%n" +
                "  -h, --help                Show this help message and exit.%n" +
                "  -n, --app-name=APPLICATION_NAME%n" +
                "                            The Name of the application to rescan%n" +
                "      --no-clone, --no-rescan, --create-new-version%n" +
                "                            Enable this flag to create a new version without%n" +
                "                              cloning the latest version configuration. Note%n" +
                "                              that when using \"in-place\" more, this parameter%n" +
                "                              will be ignore and versions will always be%n" +
                "                              cloned. if specified without parameter: true%n" +
                "      --node-name=NODE_NAME The name of the node on which the application will%n" +
                "                              be created. Ignored if no --auto-create or the%n" +
                "                              application already exists.%n" +
                "  -s, --server-url=AIP_CONSOLE_URL%n" +
                "                            The base URL for AIP Console (defaults to http:%n" +
                "                              //localhost:8081)%n" +
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
                "                              without parameter: true%n"
        );
        String actual = new CommandLine(deliverVersionCommand, springAwareCommandFactory)
                .getUsageMessage(CommandLine.Help.Ansi.OFF);
        assertThat(expected, is(actual));
    }

    @Test
    public void testDeliverVersionCommand_AddJobVersionFailed() throws ApplicationServiceException, UploadException, JobServiceException, PackagePathInvalidException {
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "--file", sflPath.toString(),
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--no-clone", "--auto-create", "--enable-security-dataflow", "--backup",
                "--backup-name", TestConstants.TEST_BACKUP_NAME,
                "--domain-name", TestConstants.TEST_DOMAIN};

        // gives the existing application
        when(applicationService.getOrCreateApplicationFromName(any(String.class), anyBoolean(), eq(null), any(String.class), anyBoolean())).thenReturn(TestConstants.TEST_APP_GUID);
        when(applicationService.getApplicationNameFromGuid(TestConstants.TEST_APP_GUID)).thenReturn(TestConstants.TEST_CREATRE_APP);
        when(applicationService.getApplicationFromName(TestConstants.TEST_CREATRE_APP)).thenReturn(AipConsoleToolsCliBaseTest.simplifiedModeApp);
        when(uploadService.uploadFileAndGetSourcePath(any(String.class), any(String.class), any(File.class))).thenReturn(sflPath.toString());
        when(applicationService.applicationHasVersion(TestConstants.TEST_APP_GUID)).thenReturn(false);
        when(applicationService.createDeliveryConfiguration(TestConstants.TEST_APP_GUID, sflPath.toString(), null, false)).thenReturn(TestConstants.TEST_DELIVERY_CONFIG_GUID);
        when(jobsService.startAddVersionJob(any(JobRequestBuilder.class))).thenReturn(null);

        JobStatusWithSteps jobStatus = new JobStatusWithSteps();
        jobStatus.setAppGuid(TestConstants.TEST_APP_GUID);
        jobStatus.setState(JobState.FAILED);
        jobStatus.setCreated(new Date());
        jobStatus.setAppName(TestConstants.TEST_CREATRE_APP);
        when(jobsService.pollAndWaitForJobFinished(eq(null), any(Function.class), anyBoolean())).thenReturn(jobStatus);

        runStringArgs(deliverVersionCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_JOB_FAILED));
    }

    @Test
    public void testDeliverVersionCommand_JobCompleted() throws ApplicationServiceException, UploadException, JobServiceException, PackagePathInvalidException {
        boolean verbose = true;
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "--file", sflPath.toString(),
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--node-name", TestConstants.TEST_NODE,
                "--no-clone", "--auto-create", "--enable-security-dataflow", "--backup",
                "--backup-name", TestConstants.TEST_BACKUP_NAME,
                "--domain-name", TestConstants.TEST_DOMAIN};

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
        when(jobsService.pollAndWaitForJobFinished(anyString(), any(Function.class), anyBoolean())).thenReturn(jobStatus);

        runStringArgs(deliverVersionCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }
}
