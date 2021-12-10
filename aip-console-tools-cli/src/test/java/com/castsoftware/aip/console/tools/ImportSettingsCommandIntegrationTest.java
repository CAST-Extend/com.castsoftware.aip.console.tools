package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.ImportSettingsCommand;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;

import static com.castsoftware.aip.console.tools.TestConstants.TEST_SRC_FOLDER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {AipConsoleToolsCliIntegrationTest.class})
@ActiveProfiles(TestConstants.PROFILE_INTEGRATION_TEST)
public class ImportSettingsCommandIntegrationTest extends AipConsoleToolsCliBaseTest {
    @Autowired
    private ImportSettingsCommand importSettingsCommand;

    @Override
    protected void cleanupTestCommand() {
        resetSharedOptions(importSettingsCommand.getSharedOptions());
        importSettingsCommand.setFilePath(null);
    }

    @Test
    public void testImportSettingsCommand_WithValidParametersAndUnExistingFile() throws ApplicationServiceException {
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "-f", TEST_SRC_FOLDER
        };

        runStringArgs(importSettingsCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_MISSING_FILE));
    }

    @Test
    public void testImportSettingsCommand_WithValidParametersAndExistingFile() throws ApplicationServiceException, IOException {
        Path exportedFromV1 = sflPath.resolve("V1-Exported-file.json");
        exportedFromV1.toFile().createNewFile();
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "-f", exportedFromV1.toString()
        };

        runStringArgs(importSettingsCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    @Test
    public void testImportSettingsCommand_WithMissingFile() throws ApplicationServiceException {
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "-f"
        };

        runStringArgs(importSettingsCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_INVALID_PARAMETERS_ERROR));
    }

}
