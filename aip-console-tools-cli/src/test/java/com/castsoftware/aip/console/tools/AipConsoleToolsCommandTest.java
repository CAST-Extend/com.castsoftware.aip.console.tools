package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.ParentCommand;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.factories.SpringAwareCommandFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Profile;
import picocli.CommandLine;

import java.util.List;
import java.util.Objects;

@SpringBootApplication
@Profile("INTEGRATION_PROFILE_TEST")
@Slf4j
public class AipConsoleToolsCommandTest implements CommandLineRunner {
    final private String[] args;
    private CommandLine cli;

    private AipConsoleToolsCommandTest() {
        this.args = new String[0];
    }

    AipConsoleToolsCommandTest(String[] args) {
        this.args = args;
    }

    private AipConsoleToolsCommandTest createCommandLine(String[] args) {
        this.cli = new CommandLine(this, springAwareCommandFactory);
        return this;
    }

    @Autowired
    private SpringAwareCommandFactory springAwareCommandFactory;

    @Autowired
    private ParentCommand parentCommand;

    public static void main(String[] args) {
        new SpringApplicationBuilder(AipConsoleToolsCommandTest.class)
                .logStartupInfo(false)
                .run(args);
/*
        new AipConsoleToolsCommandTest(args)
                .createCommandLine(args)
                .run();
 */
    }

    @Override
    public void run(String... args) {
        Integer result;

        try {
            CommandLine cli = new CommandLine(parentCommand, springAwareCommandFactory);
            cli.setUsageHelpWidth(50);
            List<Object> returnedResults = cli.parseWithHandler(new CommandLine.RunLast(), args);
            if (returnedResults != null) {
                result = returnedResults.stream()
                        .map(o -> o instanceof Integer ? (Integer) o : null)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(Constants.RETURN_OK);
            } else {
                // Help message was shown
                result = result = cli.getUnmatchedArguments().isEmpty() ? 0 : 1;
            }
        } catch (Throwable t) {
            log.error("Could not run AIP integration tool", t);
            result = Constants.UNKNOWN_ERROR;
        }
        System.exit(result);
    }
}
