package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.DeliverVersionCommand;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static com.castsoftware.aip.console.tools.TestConstants.TEST_BACKUP_NAME;
import static com.castsoftware.aip.console.tools.TestConstants.TEST_SRC_FOLDER;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class DeliverVersionCommandTest extends AipCommandTest<DeliverVersionCommand> {
    @Test
    public void testDeliverVersionCommand_WithDefaultParams() {
        String[] sb = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "--file", TEST_SRC_FOLDER, "--verbose=false",
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--no-clone",
                "--auto-create", "--enable-security-dataflow",
                "--backup",
                "--backup-name", TEST_BACKUP_NAME,
                "--domain-name", TestConstants.TEST_DOMAIN};

        aipCommandLine.parseArgs(sb);
        assertEquals(TestConstants.TEST_API_KEY, aipCommand.getSharedOptions().getApiKey());
        assertEquals(TestConstants.TEST_CREATRE_APP, aipCommand.getApplicationName());
        assertEquals(TestConstants.TEST_DOMAIN, aipCommand.getDomainName());
        assertEquals(TEST_BACKUP_NAME, aipCommand.getBackupName());
        assertEquals(TestConstants.TEST_VERSION_NAME, aipCommand.getVersionName());
        assertEquals(false, aipCommand.getSharedOptions().isVerbose());
        assertEquals(true, aipCommand.getFilePath().getAbsolutePath().endsWith(TEST_SRC_FOLDER));
        assertEquals(true, aipCommand.isBackupEnabled());
        assertEquals(true, aipCommand.isDisableClone());
        assertEquals(true, aipCommand.isAutoCreate());
    }

}
