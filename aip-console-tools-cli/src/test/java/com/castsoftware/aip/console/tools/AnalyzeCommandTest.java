package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.AnalyzeCommand;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzeCommandTest extends AipCommandTest<AnalyzeCommand> {

    @Test
    public void testAnalyzeCommand_WithDefaultParams() {
        String[] sb = new String[]{"--apikey",
                TestConstants.TEST_API_KEY, "--app-name=" + TestConstants.TEST_CREATE_APP,
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "-S",
                "--process-imaging",
        };

        aipCommandLine.parseArgs(sb);
        assertEquals(TestConstants.TEST_API_KEY, aipCommand.getSharedOptions().getApiKey());
        assertEquals(TestConstants.TEST_CREATE_APP, aipCommand.getApplicationName());
        assertEquals(TestConstants.TEST_VERSION_NAME, aipCommand.getVersionName());
        assertEquals(true, aipCommand.getSharedOptions().isVerbose());
        assertEquals(true, aipCommand.isWithSnapshot());
        assertEquals(true, aipCommand.isProcessImaging());
    }

    @Test
    public void testAnalyzeCommand_WithParamValues() {
        String[] sb = new String[]{"--apikey",
                TestConstants.TEST_API_KEY, "--app-name=" + TestConstants.TEST_CREATE_APP,
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--verbose=false",
                "-S=false",
                "--process-imaging=true"
        };

        aipCommandLine.parseArgs(sb);
        assertEquals(TestConstants.TEST_API_KEY, aipCommand.getSharedOptions().getApiKey());
        assertEquals(TestConstants.TEST_CREATE_APP, aipCommand.getApplicationName());
        assertEquals(TestConstants.TEST_VERSION_NAME, aipCommand.getVersionName());
        assertEquals(false, aipCommand.getSharedOptions().isVerbose());
        assertEquals(false, aipCommand.isWithSnapshot());
        assertEquals(true, aipCommand.isProcessImaging());
    }
}
