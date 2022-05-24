package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.DeliverVersionCommand;
import com.castsoftware.aip.console.tools.core.dto.ExclusionRuleType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.stream.Collectors;

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

    @Test
    public void testDeliverVersionCommand_ExclusionsParams() {
        String rules = "EXCLUDE_EMPTY_PROJECTS,PREFER_FULL_DOT_NET_TO_BASIC_DOT_NET_WEB,EXCLUDE_TEST_CODE";
        String[] sb = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "--file", TEST_SRC_FOLDER, "--verbose=false",
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--no-clone",
                "--auto-create", "--enable-security-dataflow",
                "--backup",
                "--backup-name", TEST_BACKUP_NAME,
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--exclusion-rules", rules
        };

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

        String parsedRules = Arrays.stream(aipCommand.getExclusionRules()).map(ExclusionRuleType::name).collect(Collectors.joining(","));
        assertEquals(rules, parsedRules);
    }

    @Test(expected = CommandLine.MissingParameterException.class)
    public void testDeliverVersionCommand_EmptyProjectExclusionRules() {
        String rules = "";
        String[] sb = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "--file", TEST_SRC_FOLDER, "--verbose=false",
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--no-clone",
                "--auto-create", "--enable-security-dataflow",
                "--backup",
                "--backup-name", TEST_BACKUP_NAME,
                "--domain-name", TestConstants.TEST_DOMAIN,
                "--exclusion-rules"
        };

        aipCommandLine.parseArgs(sb);
        String parsedRules = Arrays.stream(aipCommand.getExclusionRules()).map(ExclusionRuleType::name).collect(Collectors.joining(","));
        assertEquals(rules, parsedRules);
    }
}
