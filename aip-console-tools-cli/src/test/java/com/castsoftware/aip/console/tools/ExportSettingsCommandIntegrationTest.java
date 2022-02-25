package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.ExportSettingsCommand;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.castsoftware.aip.console.tools.TestConstants.TEST_SRC_FOLDER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {AipConsoleToolsCliIntegrationTest.class})
@ActiveProfiles(TestConstants.PROFILE_INTEGRATION_TEST)
public class ExportSettingsCommandIntegrationTest extends AipConsoleToolsCliBaseTest {
    @Autowired
    private ExportSettingsCommand exportSettingsCommand;

    @Override
    protected void cleanupTestCommand() {
        resetSharedOptions(exportSettingsCommand.getSharedOptions());
        exportSettingsCommand.setFilePath(null);
    }

    @Test
    public void testExportSettingsCommand_WithFilePathAsRelative() {
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "-f", TEST_SRC_FOLDER
        };

        runStringArgs(exportSettingsCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_INVALID_PARAMETERS_ERROR));
    }

    @Test
    public void testExportSettingsCommand_WithMissingAppList() throws IOException {
        Path existingRootPath = sflPath.resolve(TEST_SRC_FOLDER);
        Files.createDirectories(existingRootPath);
        Path existingFilePath = existingRootPath.resolve("fake_file.json");
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "-f", existingFilePath.toString()
        };

        runStringArgs(exportSettingsCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_INVALID_PARAMETERS_ERROR));
    }

    @Test
    public void testExportSettingsCommand_WithAppListExceedLimit() throws IOException {
        Path existingRootPath = sflPath.resolve(TEST_SRC_FOLDER);
        Files.createDirectories(existingRootPath);
        Path existingFilePath = existingRootPath.resolve("fake_file.json");

        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "-f", existingFilePath.toString(),
                "--appList", "A;B;C;D;E;F"
        };

        runStringArgs(exportSettingsCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_INVALID_PARAMETERS_ERROR));
    }

    @Test
    public void testExportSettingsCommand_WithAppListInWrongFormat() throws IOException {
        Path existingRootPath = sflPath.resolve(TEST_SRC_FOLDER);
        Files.createDirectories(existingRootPath);
        Path existingFilePath = existingRootPath.resolve("fake_file.json");

        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "-f", existingFilePath.toString(),
                "--appList", "A,B;C"
        };

        runStringArgs(exportSettingsCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    @Test
    public void testExportSettingsCommand_WithAlreadyExistsFile() throws IOException {
        Path existingRootPath = sflPath.resolve(TEST_SRC_FOLDER);
        Files.createDirectories(existingRootPath);
        Path existingFilePath = existingRootPath.resolve("fake_file.json");
        existingFilePath.toFile().createNewFile();

        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "-f", existingFilePath.toString(),
                "--appList", "A;B"
        };

        runStringArgs(exportSettingsCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_FILE_ALREADY_EXISTS));
    }

    @Test
    public void testExportSettingsCommand_WithFilePathAsDirectory() throws IOException {
        Path existingRootPath = sflPath.resolve(TEST_SRC_FOLDER);
        Files.createDirectories(existingRootPath);

        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "-f", existingRootPath.toString(),
                "-apps", "A;B;C"
        };

        Path expectedResultFilePath = existingRootPath.resolve(Constants.DEFAULT_EXPORTED_SETTINGS_FILENAME);
        runStringArgs(exportSettingsCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_OK));
        assertThat(expectedResultFilePath.toFile().exists(), is(true));
    }

    @Test
    public void testExportSettingsCommand_WithoutFileParameter() {
        String[] args = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "-f"
        };

        runStringArgs(exportSettingsCommand, args);
        CommandLine.Model.CommandSpec spec = cliToTest.getCommandSpec();
        assertThat(spec, is(notNullValue()));
        assertThat(exitCode, is(Constants.RETURN_INVALID_PARAMETERS_ERROR));
    }
}
