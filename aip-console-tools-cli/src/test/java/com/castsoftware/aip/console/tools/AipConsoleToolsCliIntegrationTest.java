package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.ParentCommand;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.factories.SpringAwareCommandFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import picocli.CommandLine;

//@SpringBootApplication
@Configuration
@ComponentScan
@EnableAutoConfiguration
@Profile(TestConstants.PROFILE_INTEGRATION_TEST)
public class AipConsoleToolsCliIntegrationTest implements CommandLineRunner, ExitCodeGenerator {
    private int exitCode;

    @Autowired
    private SpringAwareCommandFactory springAwareCommandFactory;

    @Autowired
    private ParentCommand parentCommand;

    @Value("${picocli.usage.width:120}")
    private int consoleUsageWidth;

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(
                new SpringApplicationBuilder(AipConsoleToolsCliIntegrationTest.class)
                        .logStartupInfo(false)
                        .run(args)));
    }

    @Override
    public void run(String... args) {
        if (args.length == 0) {
            exitCode = Constants.RETURN_OK; //SpringBootTest startup
        } else {
            exitCode = new CommandLine(parentCommand, springAwareCommandFactory)
                    .setUsageHelpWidth(consoleUsageWidth)
                    .execute(args);
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
