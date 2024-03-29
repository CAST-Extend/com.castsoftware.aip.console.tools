package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.AddVersionCommand;
import com.castsoftware.aip.console.tools.core.dto.ExclusionRuleType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class AddVersionCommandTest extends AipCommandTest<AddVersionCommand> {
    protected static final String TEST_APP_GUID = "APP-G-U-I-D";
    protected static final String TEST_DOMAIN = "TEST-DOM";
    protected static final String TEST_NODE = "TEST-NODE";
    protected static final String TEST_BACKUP_NAME = "Bak-Name";
    protected static final String TEST_VERSION_NAME = "V-1";
    protected static final String TEST_SRC_FOLDER = "SubFolder";

    @Test
    public void testAddVersionCommand_WithDefaultParams() {
        String[] sb = new String[]{"add", "--apikey",
                TestConstants.TEST_API_KEY, "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "--file", TEST_SRC_FOLDER,
                "--version-name", TEST_VERSION_NAME,
                "--no-clone", "--copy-previous-config",
                "--auto-create", "--enable-security-dataflow",
                "--process-imaging", "--backup",
                "--backup-name", TEST_BACKUP_NAME,
                "--domain-name", TEST_DOMAIN};

        aipCommandLine.parseArgs(sb);
        assertEquals(TestConstants.TEST_API_KEY, aipCommand.getSharedOptions().getApiKey());
        assertEquals(TestConstants.TEST_CREATRE_APP, aipCommand.getApplicationName());
        assertEquals(TEST_DOMAIN, aipCommand.getDomainName());
        assertEquals(TEST_BACKUP_NAME, aipCommand.getBackupName());
        assertEquals(TEST_VERSION_NAME, aipCommand.getVersionName());
        assertEquals(true, aipCommand.getSharedOptions().isVerbose());
        assertEquals(true, aipCommand.getFilePath().getAbsolutePath().endsWith(TEST_SRC_FOLDER));
        assertEquals(true, aipCommand.isBackupEnabled());
        assertEquals(true, aipCommand.isProcessImaging());
        assertEquals(true, aipCommand.isDisableClone());
        assertEquals(true, aipCommand.isAutoCreate());
    }

    @Test
    public void testAddVersionCommand_WithAliases() {
        String[] sb = new String[]{"add", "--apikey",
                TestConstants.TEST_API_KEY, "-n", TestConstants.TEST_CREATRE_APP,
                "-f", TEST_SRC_FOLDER, "-v", TEST_VERSION_NAME,
                "-a", TEST_APP_GUID,
                "--new-configuration", "-c",
                "--auto-create", "--enable-security-dataflow",
                "--process-imaging", "-b",
                "--backup-name", TEST_BACKUP_NAME,
                "--domain-name", TEST_DOMAIN
                , "--node-name", TEST_NODE};

        aipCommandLine.parseArgs(sb);
        assertEquals(TestConstants.TEST_API_KEY, aipCommand.getSharedOptions().getApiKey());
        assertEquals(TestConstants.TEST_CREATRE_APP, aipCommand.getApplicationName());
        assertEquals(TEST_DOMAIN, aipCommand.getDomainName());
        assertEquals(TEST_APP_GUID, aipCommand.getApplicationGuid());
        assertEquals(TEST_BACKUP_NAME, aipCommand.getBackupName());
        assertEquals(TEST_VERSION_NAME, aipCommand.getVersionName());
        assertEquals(TEST_NODE, aipCommand.getNodeName());
        assertEquals(true, aipCommand.getSharedOptions().isVerbose());
        assertEquals(true, aipCommand.getFilePath().getAbsolutePath().endsWith(TEST_SRC_FOLDER));
        assertEquals(true, aipCommand.isBackupEnabled());
        assertEquals(true, aipCommand.isProcessImaging());
        assertEquals(true, aipCommand.isDisableClone());
        assertEquals(true, aipCommand.isAutoCreate());
    }

    @Test
    public void testAddVersionCommand_WithSomeMissingParams() {
        String[] sb = new String[]{"add", "--apikey",
                TestConstants.TEST_API_KEY, "-n", TestConstants.TEST_CREATRE_APP,
                "-f", TEST_SRC_FOLDER, "-v", TEST_VERSION_NAME,
                "-a", TEST_APP_GUID,
                "-c", "--auto-create", "-b"};

        aipCommandLine.parseArgs(sb);
        assertEquals(TestConstants.TEST_API_KEY, aipCommand.getSharedOptions().getApiKey());
        assertEquals(TestConstants.TEST_CREATRE_APP, aipCommand.getApplicationName());
        assertEquals(true, StringUtils.isEmpty(aipCommand.getDomainName()));
        assertEquals(TEST_APP_GUID, aipCommand.getApplicationGuid());
        assertEquals(true, StringUtils.isEmpty(aipCommand.getBackupName()));
        assertEquals(TEST_VERSION_NAME, aipCommand.getVersionName());
        assertEquals(true, StringUtils.isEmpty(aipCommand.getNodeName()));
        assertEquals(true, aipCommand.getSharedOptions().isVerbose());
        assertEquals(true, aipCommand.getFilePath().getAbsolutePath().endsWith(TEST_SRC_FOLDER));
        assertEquals(true, aipCommand.isBackupEnabled());
        assertEquals(false, aipCommand.isProcessImaging());
        assertEquals(false, aipCommand.isDisableClone());
        assertEquals(true, aipCommand.isAutoCreate());
        assertEquals(false, aipCommand.isEnableSecurityDataflow());
    }

    @Test
    public void testAddVersionCommand_ExclusionsParams() {
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
    public void testAddVersionCommand_EmptyProjectExclusionRules() {
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
