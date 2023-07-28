package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.BasicCollable;
import com.castsoftware.aip.console.tools.commands.SharedOptions;
import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.services.UploadService;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.core.utils.SemVerUtils;
import com.castsoftware.aip.console.tools.factories.SpringAwareCommandFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.util.ReflectionUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import static org.mockito.Mockito.when;

@Slf4j
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
    protected Path sourceFolderPath;
    protected int exitCode;
    private List<String> unExpectedParameters;
    protected static String ARG_CONSOLIDATE_LABEL = "<consolidation>";
    protected static String ARG_IMAGING_LABEL = "<processImaging>";

    @Before
    public void startup() throws IOException {
        initializePrivateMocks();

        sflPath = folder.getRoot().toPath().resolve("SFL");
        sourceFolderPath = Paths.get("testFolder");
        Files.createDirectories(sflPath);
        zippedSourcesPath = sflPath.resolve("fake_sources.zip");
        zippedSourcesPath.toFile().createNewFile();
        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion(SemVerUtils.getMinCompatibleVersion().toString()).build();
        when(restApiService.getAipConsoleApiInfo()).thenReturn(apiInfoDto);
        when(applicationService.getAipConsoleApiInfo()).thenReturn(apiInfoDto);
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

        //Others startup stubbing
        additionalStartup();
    }

    protected void additionalStartup() throws IOException {
        // Add others stub below
    }

    protected abstract void cleanupTestCommand();

    protected void initializePrivateMocks() {
    }

    protected List<String> getUnExpectedParameters() {
        return unExpectedParameters;
    }

    protected void resetSharedOptions(SharedOptions command) {
        if (unExpectedParameters != null) {
            unExpectedParameters.clear();
        }
        command.setApiKey(null);
        command.setApiKeyEnvVariable(null);
        command.setServerRootUrl(null);
        command.setUsername(null);
        if (command.getUnmatchedOptions() != null) {
            command.getUnmatchedOptions().clear();
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

    protected void assignMockedBeans(BasicCollable command) {
        Class<? extends BasicCollable> commandClass = command.getClass();
        Field jobsServiceField = ReflectionUtils.findField(commandClass, "jobsService");
        ReflectionUtils.makeAccessible(jobsServiceField);
        ReflectionUtils.setField(jobsServiceField, command, jobsService);

        Field restApiServiceField = ReflectionUtils.findField(commandClass, "restApiService");
        ReflectionUtils.makeAccessible(restApiServiceField);
        ReflectionUtils.setField(restApiServiceField, command, restApiService);

        Field applicationServiceField = ReflectionUtils.findField(commandClass, "applicationService");
        ReflectionUtils.makeAccessible(applicationServiceField);
        ReflectionUtils.setField(applicationServiceField, command, applicationService);
    }

    protected ApplicationDto getTestApplicationMock() {
        ApplicationDto applicationDto = Mockito.mock(ApplicationDto.class);
        when(applicationDto.getName()).thenReturn(TestConstants.TEST_CREATRE_APP);
        when(applicationDto.getGuid()).thenReturn(TestConstants.TEST_APP_GUID);
        return applicationDto;
    }

    protected CommandLine.Model.OptionSpec getCommandLineOption(CommandLine.Model.CommandSpec spec, String optionName) {
        //Checks that the initial value set for the module type is full content
        List<CommandLine.Model.ArgSpec> argsSpec = spec.args();
        for (CommandLine.Model.ArgSpec cmdArg : argsSpec) {
            CommandLine.Model.OptionSpec optionSpec = (CommandLine.Model.OptionSpec) cmdArg;
            if (StringUtils.equalsAny(optionName, optionSpec.names())) {
                return optionSpec;
            }
        }
        return null;
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
                if (cliToTest.getExecutionResult() == null) {
                    exitCode = Constants.RETURN_INVALID_PARAMETERS_ERROR;
                } else {
                    exitCode = unExpectedParameters.isEmpty() ? 0 : 1;
                }
            }
        } catch (Throwable t) {
            log.error(t.getMessage());
            exitCode = Constants.UNKNOWN_ERROR;
        }
    }
    protected boolean getBooleanArgValue(CommandLine.Model.ArgSpec arg) {
        if (arg.typedValues().isEmpty()) {
            return (Boolean) arg.initialValue();
        }
        return (Boolean) arg.typedValues().get(0);
    }
}
