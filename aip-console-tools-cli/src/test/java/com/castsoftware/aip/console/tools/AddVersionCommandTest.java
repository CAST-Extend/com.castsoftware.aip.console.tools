package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.AddVersionCommand;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.services.ApplicationServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.text.ParseException;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AddVersionCommandTest extends AipCommandTest<AddVersionCommand> {
    protected static final String TEST_APP_GUID = "APP-G-U-I-D";
    protected static final String TEST_DOMAIN = "TEST-DOM";
    protected static final String TEST_NODE = "TEST-NODE";
    protected static final String TEST_BACKUP_NAME = "Bak-Name";
    protected static final String TEST_VERSION_NAME = "V-1";
    protected static final String TEST_SRC_FOLDER = "SubFolder";

    @Mock
    private ApplicationServiceImpl applicationService;

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
    public void testServiceVersionDateConversion() throws ApplicationServiceException, ParseException {
        //For UI when we provide 2022-07-15 15:21 it sets releaseDate to 2022-07-15T13:21:33.140Z
        when(applicationService.getVersionDate(anyString())).thenCallRealMethod();
        Date cliServiceDate = applicationService.getVersionDate("2022-07-15 15:21:01");
        assertEquals("Fri Jul 15 13:21:01 CEST 2022", cliServiceDate.toString());
    }
}
