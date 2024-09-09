package com.castsoftware.aip.console.tools.commands.TccCommands;

import com.castsoftware.aip.console.tools.commands.BasicCallable;
import com.castsoftware.aip.console.tools.commands.SharedOptions;
import com.castsoftware.aip.console.tools.core.dto.tcc.ComputeFunctionPointsProperties;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.utils.VersionInformation;
import com.castsoftware.aip.console.tools.providers.CliLogPollingProviderImpl;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(name = "compute-transactions", mixinStandardHelpOptions = true, description = "Compute the transactions for the specified application.")
@Slf4j
@Getter
@Setter
public class ComputeTransactions extends BasicCallable {
    @CommandLine.ParentCommand
    private TccCommand parentCommand;

    @CommandLine.Option(names = "--wait", paramLabel = "WAIT", description = "Wait for the compute to finish.", negatable = true, defaultValue = "true", fallbackValue = "true")
    boolean wait;

    public ComputeTransactions(RestApiService restApiService, JobsService jobsService, ApplicationService applicationService) {
        super(restApiService, jobsService, applicationService);
    }

    @Override
    public Integer processCallCommand() throws Exception {
        ComputeFunctionPointsProperties computeFunctionPointsProperties = ComputeFunctionPointsProperties.builder()
                .applicationName(parentCommand.getApplicationName())
                .wait(wait)
                .verbose(getSharedOptions().isVerbose())
                .logPollingProvider(wait ? new CliLogPollingProviderImpl(jobsService,
                        getSharedOptions().isVerbose(), getSharedOptions().getSleepDuration()) : null)
                .build();
        return applicationService.computeFunctionPoints(computeFunctionPointsProperties);
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
