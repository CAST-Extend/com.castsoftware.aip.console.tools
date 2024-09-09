package com.castsoftware.aip.console.tools.commands.TccCommands;

import com.castsoftware.aip.console.tools.commands.BasicCallable;
import com.castsoftware.aip.console.tools.commands.SharedOptions;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.services.UploadService;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.core.utils.VersionInformation;
import com.castsoftware.aip.console.tools.factories.SpringAwareCommandFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "TCC",
        mixinStandardHelpOptions = true,
        aliases = {"TCC"},
        subcommands = {
                ComputeFunctionPoints.class,
                ComputeTransactions.class,
                ListFunctionPointRules.class,
                CheckContent.class,
                CheckAllContent.class,
                ViewSettings.class,
                UpdateSettings.class
        },
        //edit description to match v3
        description = "Performs Transaction Call Graph(TCC) operations"
)
@Slf4j
@Getter
@Setter
public class TccCommand extends BasicCallable {
    @CommandLine.Option(names = {"-n", "--app-name"}, paramLabel = "APPLICATION_NAME",
            description = "The name of the app on which the TCC operation has to be performed.",
            required = true, scope = CommandLine.ScopeType.INHERIT)
    protected String applicationName;

    @CommandLine.Mixin
    protected SharedOptions sharedOptions;

    private static final VersionInformation MIN_VERSION = VersionInformation.fromVersionString("3.0.0");

    public TccCommand(RestApiService restApiService, JobsService jobsService, UploadService uploadService, ApplicationService applicationService) {
        super(restApiService, jobsService, uploadService, applicationService);
    }

    @Autowired
    private SpringAwareCommandFactory springAwareCommandFactory;

    @Override
    public Integer processCallCommand() throws Exception {
        log.error("Use TCC command with its subcommands.");
        CommandLine cli = new CommandLine(this, springAwareCommandFactory);
        cli.usage(System.out);
        return Constants.RETURN_OK;
    }

    @Override
    protected VersionInformation getMinVersion() {
        return MIN_VERSION;
    }


}