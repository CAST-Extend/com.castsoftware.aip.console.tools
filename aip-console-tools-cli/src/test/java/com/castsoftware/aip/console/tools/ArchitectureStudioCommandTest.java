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
        String[] sb = new String[]{
                "--apikey", TestConstants.TEST_API_KEY,
                "--app-name", TestConstants.TEST_CREATE_APP,
                "--model-name", "Demo",
                "--file-path", "path"
        };

        aipCommandLine.parseArgs(sb);
        assertEquals(TestConstants.TEST_CREATE_APP, aipCommand.getApplicationName());
    }

}
