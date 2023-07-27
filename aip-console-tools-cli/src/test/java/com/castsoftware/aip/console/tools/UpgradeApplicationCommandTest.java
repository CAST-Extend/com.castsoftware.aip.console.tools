package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.UpgradeApplicationCommand;
import com.castsoftware.aip.console.tools.factories.SpringAwareCommandFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import picocli.CommandLine;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class UpgradeApplicationCommandTest {
    private static final String TEST_APP_NAME = "App-name";
    private static final String TEST_API_KEY = "API-Key";

    private AnnotationConfigApplicationContext context;
    private CommandLine upgradeAppCmdLine;
    private UpgradeApplicationCommand upgradeAppCmd;

    @Before
    public void startup() {
        load(AipIntegrationCliMain.class);
        CommandLine.IFactory factory = this.context.getBean(SpringAwareCommandFactory.class);
        upgradeAppCmd = this.context.getBean(UpgradeApplicationCommand.class);
        upgradeAppCmdLine = new CommandLine(upgradeAppCmd, factory);
    }

    @After
    public void tearDown() {
        if (this.context != null) {
            this.context.close();
        }
    }

    @Test
    public void testUpgradeApplicationCommand() {
        String[] sb = new String[]{
                "--apikey", TEST_API_KEY,
                "--app-name", TEST_APP_NAME
        };
        upgradeAppCmdLine.parseArgs(sb);
        assertEquals(TEST_API_KEY, upgradeAppCmd.getSharedOptions().getApiKey());
        assertEquals(TEST_APP_NAME, upgradeAppCmd.getAppName());
        assertEquals(true, upgradeAppCmd.getSharedOptions().isVerbose());
    }

    @Test
    public void testUpgradeApplicationCommand_withAlias() {
        String[] sb = new String[]{"--apikey", TEST_API_KEY, "-n", TEST_APP_NAME, "--verbose=false"};

        upgradeAppCmdLine.parseArgs(sb);
        assertEquals(TEST_API_KEY, upgradeAppCmd.getSharedOptions().getApiKey());
        assertEquals(TEST_APP_NAME, upgradeAppCmd.getAppName());
        assertEquals(false, upgradeAppCmd.getSharedOptions().isVerbose());
    }

    private void load(Class<?> config, String... environment) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(config);
        applicationContext.refresh();
        this.context = applicationContext;
    }


}
