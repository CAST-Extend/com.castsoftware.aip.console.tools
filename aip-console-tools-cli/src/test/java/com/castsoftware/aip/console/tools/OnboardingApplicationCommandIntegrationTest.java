package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.OnboardApplicationCommand;
import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import picocli.CommandLine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {AipConsoleToolsCliIntegrationTest.class})
@ActiveProfiles(TestConstants.PROFILE_INTEGRATION_TEST)
public class OnboardingApplicationCommandIntegrationTest extends AipConsoleToolsCliBaseTest {
    @Autowired
    private OnboardApplicationCommand onboardApplicationCommand;

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
        resetSharedOptions(onboardApplicationCommand.getSharedOptions());
        onboardApplicationCommand.setApplicationName(null);
        onboardApplicationCommand.setDomainName(null);
        onboardApplicationCommand.setFilePath(null);
        onboardApplicationCommand.setNodeName(null);
    }

    @Test
    public void testOnboardingApplication_WithNotCompatibleVersion() throws Exception {
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY, "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "-f", zippedSourcesPath.toString(),
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--node-name", TestConstants.TEST_NODE};


        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion("2.4.9-funcrel").build();
        doReturn(apiInfoDto).when(restApiService).getAipConsoleApiInfo();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();
        when(restApiService.getForEntity("/api/", ApiInfoDto.class)).thenReturn(apiInfoDto);

        runStringArgs(onboardApplicationCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_SERVER_VERSION_NOT_COMPATIBLE));
    }

    @Test
    public void testOnboardingApplication_WithCompatibleVersion() throws Exception {
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "-f", zippedSourcesPath.toString(),
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--node-name", TestConstants.TEST_NODE};

        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion("2.5.2-SNAPSHOT-133").build();
        doReturn(apiInfoDto).when(restApiService).getAipConsoleApiInfo();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();
        when(restApiService.getForEntity("/api/", ApiInfoDto.class)).thenReturn(apiInfoDto);

        runStringArgs(onboardApplicationCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_APPLICATION_INFO_MISSING));

    }

}
