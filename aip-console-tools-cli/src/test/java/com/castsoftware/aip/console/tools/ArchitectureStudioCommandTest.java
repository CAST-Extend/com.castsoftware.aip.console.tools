package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.ArchitectureStudioCommand;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ArchitectureStudioCommandTest extends AipCommandTest<ArchitectureStudioCommand> {

    @Test
    public void testArchitectureStudio_withDefaultParams(){
        String[] sb = new String[]{"Arch-Studio", "--apikey",
                TestConstants.TEST_API_KEY, "--app-name=", TestConstants.TEST_CREATRE_APP,
                "--model-name=", "Demo"
        };

        aipCommandLine.parseArgs(sb);
        assertEquals(TestConstants.TEST_CREATRE_APP, aipCommand.getApplicationName());
    }
}
