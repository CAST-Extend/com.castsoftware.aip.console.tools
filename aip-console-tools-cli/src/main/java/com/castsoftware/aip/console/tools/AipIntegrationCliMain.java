package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.ParentCommand;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.factories.SpringAwareCommandFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Profile;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SpringBootApplication
@Slf4j
@Profile(Constants.EXECUTION_PROFILE_DEFAULT)
public class AipIntegrationCliMain implements CommandLineRunner {

    @Autowired
    private SpringAwareCommandFactory springAwareCommandFactory;

    @Autowired
    private ParentCommand parentCommand;

    @Value("${picocli.usage.width:120}")
    private int consoleUsageWidth;

    private List<String> unExpectedParameters;

    protected List<String> getUnExpectedParameters() {
        return unExpectedParameters;
    }

    public static void main(String... args) {
        new SpringApplicationBuilder(AipIntegrationCliMain.class)
                .logStartupInfo(false)
                .run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        Integer result;

        String VERSION_OPTION = "--version";
        try {
            String[] argsOptions = args;
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
                //Call to getUnmatchedArguments() seems to clear the content
                unExpectedParameters = new ArrayList<>(cli.getUnmatchedArguments());
                if (cli.getExecutionResult() == null) {
                    boolean askForVersion = argsOptions.length == 1 ? StringUtils.equalsIgnoreCase(VERSION_OPTION, argsOptions[0]) : false;
                    result = askForVersion ? 0 : Constants.RETURN_INVALID_PARAMETERS_ERROR;
                } else {
                    result = unExpectedParameters.isEmpty() ? 0 : 1;
                }
            }
        } catch (Throwable t) {
            log.error("Could not run AIP integration tool", t);
            result = Constants.UNKNOWN_ERROR;
        }
        System.exit(result);
    }
}
