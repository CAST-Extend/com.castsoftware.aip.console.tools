package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.CreateApplicationCommand;
import com.castsoftware.aip.console.tools.factories.SpringAwareCommandFactory;
import org.junit.After;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import picocli.CommandLine;
import picocli.spring.PicocliSpringFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AipCommandsTest {
    private AnnotationConfigApplicationContext context;

    @After
    public void tearDown() {
        if (this.context != null) {
            this.context.close();
        }
    }

    @Test
    public void testSpringAwareCommandFactory() {
        load(AipIntegrationCliMain.class);
        CommandLine.IFactory factory = this.context.getBean(SpringAwareCommandFactory.class);
        assertNotNull(factory);
        assertTrue(factory instanceof SpringAwareCommandFactory);
    }

    @Test
    public void testPicocliSpringFactory() {
        load(AipIntegrationCliMain.class);
        CommandLine.IFactory factory = this.context.getBean(PicocliSpringFactory.class);
        assertNotNull(factory);
        assertTrue(factory instanceof PicocliSpringFactory);
    }

    @Test
    public void testCreateApplicationCommand() {
        load(AipIntegrationCliMain.class);
        CommandLine.IFactory factory = this.context.getBean(SpringAwareCommandFactory.class);
        CreateApplicationCommand createAppCmd = this.context.getBean(CreateApplicationCommand.class);
        CommandLine cmd = new CommandLine(createAppCmd, factory);
        String TEST_CREATRE_APP = "To_Create_App-name";
        String[] sb = new String[]{"new", "--apikey", "API-Key", "--app-name", TEST_CREATRE_APP, "--inplace-mode", "true", "--verbose", "false"};
        cmd.parseArgs(sb);
        assertEquals(TEST_CREATRE_APP, createAppCmd.getApplicationName());
        assertEquals(true, createAppCmd.isInPlaceMode());
        //assertEquals(false, createAppCmd.getSharedOptions().isVerbose());
    }

    private void load(Class<?> config, String... environment) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        //EnvironmentTestUtils.addEnvironment(applicationContext, environment);
        applicationContext.register(config);
        applicationContext.refresh();
        this.context = applicationContext;
    }


}
