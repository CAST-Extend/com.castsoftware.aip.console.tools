package com.castsoftware.uc.aip.console.tools;

import com.castsoftware.uc.aip.console.tools.commands.ParentCommand;
import com.castsoftware.uc.aip.console.tools.core.utils.Constants;
import com.castsoftware.uc.aip.console.tools.factories.SpringAwareCommandFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import picocli.CommandLine;

import java.util.List;
import java.util.Objects;

@SpringBootApplication
@Slf4j
public class AipIntegrationCliMain implements CommandLineRunner {

    @Autowired
    private SpringAwareCommandFactory springAwareCommandFactory;

    @Autowired
    private ParentCommand parentCommand;

    @Value("${picocli.usage.width:120}")
    private int consoleUsageWidth;

    public static void main(String... args) {
        new SpringApplicationBuilder(AipIntegrationCliMain.class)
                .logStartupInfo(false)
                .run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        Integer result;

        try {
            CommandLine cli = new CommandLine(parentCommand, springAwareCommandFactory);
            cli.setUsageHelpWidth(consoleUsageWidth);

            List<Object> returnedResults = cli.parseWithHandler(new CommandLine.RunLast(), args);
            if (returnedResults != null) {
                result = returnedResults.stream()
                        .map(o -> o instanceof Integer ? (Integer) o : null)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(Constants.RETURN_OK);
            } else {
                // Help message was shown
                result = 0;
            }
        } catch (Throwable t) {
            log.error("Could not run AIP integration tool", t);
            result = Constants.UNKNOWN_ERROR;
        }
        System.exit(result);
    }
}
