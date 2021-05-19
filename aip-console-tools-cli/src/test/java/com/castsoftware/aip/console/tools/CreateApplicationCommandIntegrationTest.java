package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.CreateApplicationCommand;
import com.castsoftware.aip.console.tools.core.dto.NodeDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobStatusWithSteps;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.factories.SpringAwareCommandFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {AipConsoleToolsCliIntegrationTest.class})
@ActiveProfiles(TestConstants.PROFILE_INTEGRATION_TEST)
public class CreateApplicationCommandIntegrationTest extends AipConsoleToolsCliBaseTest {
    @Autowired
    private CreateApplicationCommand createApplicationCommand;
    @Autowired
    private SpringAwareCommandFactory springAwareCommandFactory;

    @Override
    protected void cleanupTestCommant() {
        resetSharedOptions(createApplicationCommand.getSharedOptions());
        createApplicationCommand.setApplicationName(null);
        createApplicationCommand.setDomainName(null);
        createApplicationCommand.setNodeName(null);
    }

    @Test
    public void testCreateApplicationCommandUsageHelp() {
        String expected = String.format("" +
                "Usage: CreateApplication [-hV] [--inplace-mode] [--verbose] [--apikey%n" +
                "                         [=<apiKey>]] [--apikey:env=ENV_VAR_NAME]%n" +
                "                         [--domain-name=DOMAIN_NAME] -n=APPLICATION_NAME%n" +
                "                         [--node-name=NODE_NAME] [-s=AIP_CONSOLE_URL]%n" +
                "                         [--timeout=<timeout>] [--user=<username>]%n" +
                "Creates a new application on AIP Console%n" +
                "      --apikey[=<apiKey>]   The API Key to access AIP Console. Will prompt%n" +
                "                              entry if no value is passed.%n" +
                "      --apikey:env=ENV_VAR_NAME%n" +
                "                            The name of the environment variable containing the%n" +
                "                              AIP Key to access AIP Console%n" +
                "      --domain-name=DOMAIN_NAME%n" +
                "                            The name of the domain to assign to the%n" +
                "                              application. Will be created if it doesn't%n" +
                "                              exists. No domain will be assigned if left empty.%n" +
                "  -h, --help                Show this help message and exit.%n" +
                "      --inplace-mode        If true then no history will be kept for delivered%n" +
                "                              sources. if specified without parameter: true%n" +
                "  -n, --app-name=APPLICATION_NAME%n" +
                "                            The name of the application to create%n" +
                "      --node-name=NODE_NAME The name of the node on which the application will%n" +
                "                              be created.%n" +
                "  -s, --server-url=AIP_CONSOLE_URL%n" +
                "                            The base URL for AIP Console (defaults to http:%n" +
                "                              //localhost:8081)%n" +
                "      --timeout=<timeout>   The timeout in seconds for calls to AIP Console.%n" +
                "                              Defaults to a 90s timeout%n" +
                "      --user=<username>     User name. Use this if no API Key generation is%n" +
                "                              available on AIP Console. Provide the user's%n" +
                "                              password in the apikey parameter.%n" +
                "  -V, --version             Print version information and exit.%n" +
                "      --verbose             Whether the command log should be output to the%n" +
                "                              console or not, defaulted to true if specified%n" +
                "                              without parameter: true%n"
        );
        String actual = new CommandLine(createApplicationCommand, springAwareCommandFactory)
                .getUsageMessage(CommandLine.Help.Ansi.OFF);
        assertThat(expected, is(actual));
    }

    @Test
    public void testCreateApplicationCommand_badNodeName() throws ApiCallException {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "-n", TestConstants.TEST_CREATRE_APP,
                "--node-name", TestConstants.TEST_NODE};

        NodeDto[] existingNodes = new NodeDto[]{new NodeDto("Unexisting_guid", "Unexisting_name", "Fake_scheme", "Unexisting_host", 000)};
        when(restApiService.getForEntity("/api/nodes", NodeDto[].class)).thenReturn(existingNodes);

        runStringArgs(createApplicationCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_APPLICATION_NOT_FOUND));
    }

    @Test
    public void testCreateApplicationCommand_SimplifiedDeliveryMode_DefaultNode() throws JobServiceException {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "-n", TestConstants.TEST_CREATRE_APP,
                "--inplace-mode",
                "--domain-name", TestConstants.TEST_DOMAIN};

        // gives the existing application
        JobStatusWithSteps jobStatus = new JobStatusWithSteps();
        jobStatus.setAppGuid(TestConstants.TEST_APP_GUID);
        jobStatus.setState(JobState.COMPLETED);
        jobStatus.setCreated(new Date());
        jobStatus.setAppName(TestConstants.TEST_CREATRE_APP);

        when(jobsService.startCreateApplication(any(String.class), eq(null), any(String.class), anyBoolean())).thenReturn(TestConstants.TEST_JOB_GUID);
        when(jobsService.pollAndWaitForJobFinished(any(String.class), any(Function.class), anyBoolean())).thenReturn(Constants.RETURN_OK);

        runStringArgs(createApplicationCommand, args);

        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }
}
