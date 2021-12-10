package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.AddVersionCommand;
import com.castsoftware.aip.console.tools.commands.ImportSettingsCommand;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static com.castsoftware.aip.console.tools.TestConstants.TEST_SRC_FOLDER;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ImportSettingsCommandTest  extends AipCommandTest<ImportSettingsCommand> {
    @Test
    public void testImportSettingsCommand_WithTypicalParams() {
        String[] sb = new String[]{"--apikey",
                TestConstants.TEST_API_KEY,
                "-f",TEST_SRC_FOLDER
        };

        aipCommandLine.parseArgs(sb);
        assertEquals(TestConstants.TEST_API_KEY, aipCommand.getSharedOptions().getApiKey());
        assertEquals(true, aipCommand.getSharedOptions().isVerbose());
    }

    @Test(expected=picocli.CommandLine.MissingParameterException.class)
    public void testImportSettingsCommand_WithMissingFile() {
        String[] sb = new String[]{"--apikey",
                TestConstants.TEST_API_KEY
        };

        aipCommandLine.parseArgs(sb);
    }
}
