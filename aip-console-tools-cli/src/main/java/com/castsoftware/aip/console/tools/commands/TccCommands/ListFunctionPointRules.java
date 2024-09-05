package com.castsoftware.aip.console.tools.commands.TccCommands;

import com.castsoftware.aip.console.tools.commands.BasicCallable;
import com.castsoftware.aip.console.tools.commands.SharedOptions;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.utils.VersionInformation;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(name = "list-rules", mixinStandardHelpOptions = true, description = "List the configuration rules available.")
@Slf4j
@Getter
@Setter
public class ListFunctionPointRules extends BasicCallable {
    @CommandLine.ParentCommand
    private TccCommand parentCommand;

    @CommandLine.Option(names = "--ruleType", paramLabel = "RULE_TYPE", description = "The type of rules you want to see")
    String ruleType = null;

    public ListFunctionPointRules(RestApiService restApiService, JobsService jobsService, ApplicationService applicationService) {
        super(restApiService, jobsService, applicationService);
    }

    @Override
    public Integer processCallCommand() throws Exception {
        return applicationService.listFunctionPointRules(parentCommand.getApplicationName(), ruleType);
    }

    @Override
    public SharedOptions getSharedOptions() {
        return parentCommand.getSharedOptions();
    }

    @Override
    protected VersionInformation getMinVersion() {
        return parentCommand.getMinVersion();
    }
}
