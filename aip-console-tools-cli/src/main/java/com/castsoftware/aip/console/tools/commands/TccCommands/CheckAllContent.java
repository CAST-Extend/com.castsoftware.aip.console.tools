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
@CommandLine.Command(name = "check-all-content", mixinStandardHelpOptions = true, description = "Shows all the content of a particular rule type.")
@Slf4j
@Getter
@Setter
public class CheckAllContent extends BasicCallable {
    @CommandLine.ParentCommand
    private TccCommand parentCommand;

    @CommandLine.Option(names = "--ruleType", paramLabel = "RULE_TYPE", description = "The rule type whose contents have to be shown.", required = true)
    String ruleType;

    public CheckAllContent(RestApiService restApiService, JobsService jobsService, ApplicationService applicationService) {
        super(restApiService, jobsService, applicationService);
    }

    @Override
    public Integer processCallCommand() throws Exception {
        return applicationService.checkRuleContent(parentCommand.getApplicationName(), null, ruleType);
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
