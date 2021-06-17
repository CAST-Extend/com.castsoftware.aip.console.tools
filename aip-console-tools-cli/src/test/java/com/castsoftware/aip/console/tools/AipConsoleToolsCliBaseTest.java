package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.SharedOptions;
import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.services.UploadService;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.factories.SpringAwareCommandFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import static org.mockito.Mockito.when;

public abstract class AipConsoleToolsCliBaseTest {
    @Autowired
    protected SpringAwareCommandFactory springAwareCommandFactory;

    private int consoleUsageWidth = 120;
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @MockBean
    protected RestApiService restApiService;
    @MockBean
    protected JobsService jobsService;
    @MockBean
    protected UploadService uploadService;
    @MockBean
    protected ApplicationService applicationService;

    protected CommandLine cliToTest;
    protected static final ApplicationDto simplifiedModeApp = ApplicationDto.builder().guid(TestConstants.TEST_APP_GUID).name(TestConstants.TEST_CREATRE_APP).inPlaceMode(true).build();
    protected String[] defaultArgs;
    protected Path sflPath;
    protected Path zippedSourcesPath;
    protected int exitCode;
    private List<String> unExpectedParameters;

    @Before
    public void startup() throws IOException {
        sflPath = folder.getRoot().toPath().resolve("SFL");
        Files.createDirectories(sflPath);
        zippedSourcesPath = sflPath.resolve("fake_sources.zip");
        zippedSourcesPath.toFile().createNewFile();
        when(restApiService.getAipConsoleApiInfo()).thenReturn(ApiInfoDto.builder().apiVersion("1.23.0").build());
        defaultArgs = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "--file", sflPath.toString(),
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--no-clone", "--copy-previous-config",
                "--auto-create", "--enable-security-dataflow",
                "--process-imaging", "--backup",
                "--backup-name", TestConstants.TEST_BACKUP_NAME,
                "--domain-name", TestConstants.TEST_DOMAIN};
    }

    protected abstract void cleanupTestCommand();

    protected List<String> getUnExpectedParameters() {
        return unExpectedParameters;
    }

    protected void resetSharedOptions(SharedOptions command) {
        if (unExpectedParameters != null) {
            unExpectedParameters.clear();
        }
        if (command != null) {
            command.setApiKey(null);
            command.setApiKeyEnvVariable(null);
            command.setServerRootUrl(null);
            command.setUsername(null);
            if (command.getUnmatchedOptions() != null) {
                command.getUnmatchedOptions().clear();
            }
        }
    }

    @After
    public void after() {
        // ===================================
        //command not recreated between test.
        //So just clear the command as if it was brand newly created
        //The best way is to run java -jar .\aip-console-tools-cli.jar add ...
        // So that app creates new instances of commands.
        // Still this woks fine renewing parameters values each time.
        // Here only String types, but each test should set velue to requested ones
        // ===================================
        cleanupTestCommand();
        folder.delete();
    }

    protected void runStringArgs(Callable<Integer> command, String[] args) {
        try {
            cliToTest = new CommandLine(command, springAwareCommandFactory);
            cliToTest.setUsageHelpWidth(consoleUsageWidth);
            List<Object> returnedResults = cliToTest.parseWithHandler(new CommandLine.RunLast(), args);
            if (returnedResults != null) {
                exitCode = returnedResults.stream()
                        .map(o -> o instanceof Integer ? (Integer) o : null)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(Constants.RETURN_OK);
            } else {
                // Help message was shown
                //Call to getUnmatchedArguments() seems to clear the content
                unExpectedParameters = new ArrayList<>(cliToTest.getUnmatchedArguments());
                exitCode = unExpectedParameters.isEmpty() ? 0 : 1;
            }
        } catch (Throwable t) {
            exitCode = Constants.UNKNOWN_ERROR;
        }
    }
}
