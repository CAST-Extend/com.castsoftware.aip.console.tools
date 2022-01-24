package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.ExportSettingsCommand;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static com.castsoftware.aip.console.tools.TestConstants.TEST_SRC_FOLDER;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ExportSettingsCommandTest extends AipCommandTest<ExportSettingsCommand> {
    @Test
    public void testExportSettingsCommand_WithTypicalParams() {
        String[] sb = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "-f", TEST_SRC_FOLDER
        };

        aipCommandLine.parseArgs(sb);
        assertEquals(TestConstants.TEST_API_KEY, aipCommand.getSharedOptions().getApiKey());
        assertEquals(true, aipCommand.getSharedOptions().isVerbose());
    }

    @Test(expected = picocli.CommandLine.MissingParameterException.class)
    public void testExportSettingsCommand_WithMissingFile() {
        String[] sb = new String[]{"--apikey",
                TestConstants.TEST_API_KEY
        };

        aipCommandLine.parseArgs(sb);
    }
}
