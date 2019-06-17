package com.castsoftware.uc.aip.console.tools.commands;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * This class represent a "main" command that will then delegate to subcommands
 * based on passed parameter
 */
@Component
@Command(
        name = "aip-integration-tool",
        mixinStandardHelpOptions = true,
        subcommands = {CreateApplicationCommand.class, AddVersionCommand.class},
        commandListHeading = "%nPossible values for COMMAND:%n%n"
)
@Getter
@Setter
@Slf4j
public class ParentCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        log.error("No COMMAND provided");
        CommandLine.usage(this, System.out);
        return 0;
    }
}
