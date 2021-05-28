package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.SnapshotCommand;
import org.junit.Test;
import picocli.CommandLine;

import static org.junit.Assert.assertEquals;

public class SnapshotCommandTest extends AipCommandTest<SnapshotCommand> {
    @Test
    public void testSnapshotCommand_WithDefaultParams() {
        String[] sb = new String[]{"--apikey",
                TestConstants.TEST_API_KEY, "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "-S", TestConstants.TEST_SNAPSHOT_NAME,
                "--process-imaging",
        };

        aipCommandLine.parseArgs(sb);
        assertEquals(TestConstants.TEST_API_KEY, aipCommand.getSharedOptions().getApiKey());
        assertEquals(TestConstants.TEST_CREATRE_APP, aipCommand.getApplicationName());
        assertEquals(TestConstants.TEST_VERSION_NAME, aipCommand.getVersionName());
        assertEquals(TestConstants.TEST_SNAPSHOT_NAME, aipCommand.getSnapshotName());
        assertEquals(true, aipCommand.getSharedOptions().isVerbose());
        assertEquals(true, aipCommand.isProcessImaging());
    }

    @Test(expected = CommandLine.MissingParameterException.class)
    public void testSnapshotCommand_WithMissingRequiredParams() {
        //Missing the application name
        String[] sb = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "-S", TestConstants.TEST_SNAPSHOT_NAME,
                "--process-imaging",
        };

        aipCommandLine.parseArgs(sb);
    }
}
