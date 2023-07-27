package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.ResyncApplicationCommand;
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
public class ResyncApplicationCommandTest {
    private static final String TEST_APP_NAME = "App-name";
    private static final String TEST_API_KEY = "API-Key";

    private AnnotationConfigApplicationContext context;
    private CommandLine resyncAppCmdLine;
    private ResyncApplicationCommand resyncAppCmd;

    @Before
    public void startup() {
        load(AipIntegrationCliMain.class);
        CommandLine.IFactory factory = this.context.getBean(SpringAwareCommandFactory.class);
        resyncAppCmd = this.context.getBean(ResyncApplicationCommand.class);
        resyncAppCmdLine = new CommandLine(resyncAppCmd, factory);
    }

    @After
    public void tearDown() {
        if (this.context != null) {
            this.context.close();
        }
    }

    @Test
    public void testResyncApplicationCommand() {
        String[] sb = new String[]{
                "--apikey", TEST_API_KEY,
                "--app-name", TEST_APP_NAME
        };
        resyncAppCmdLine.parseArgs(sb);
        assertEquals(TEST_API_KEY, resyncAppCmd.getSharedOptions().getApiKey());
        assertEquals(TEST_APP_NAME, resyncAppCmd.getAppName());
        assertEquals(true, resyncAppCmd.getSharedOptions().isVerbose());
    }

    @Test
    public void testResyncApplicationCommand_withAlias() {
        String[] sb = new String[]{"--apikey", TEST_API_KEY, "-n", TEST_APP_NAME, "--verbose=false"};

        resyncAppCmdLine.parseArgs(sb);
        assertEquals(TEST_API_KEY, resyncAppCmd.getSharedOptions().getApiKey());
        assertEquals(TEST_APP_NAME, resyncAppCmd.getAppName());
        assertEquals(false, resyncAppCmd.getSharedOptions().isVerbose());
    }

    private void load(Class<?> config, String... environment) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(config);
        applicationContext.refresh();
        this.context = applicationContext;
    }


}
