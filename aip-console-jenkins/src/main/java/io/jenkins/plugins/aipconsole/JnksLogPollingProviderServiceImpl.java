package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.jobs.JobExecutionDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.LogPollingProvider;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.PrintStream;

import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_jobFailure;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_success_analysisComplete;
import static io.jenkins.plugins.aipconsole.Messages.JobsSteps_changed;

public class JnksLogPollingProviderServiceImpl implements LogPollingProvider {
    private final JobsService jobsService;
    private final PrintStream log;
    private final boolean verbose;
    private Run<?, ?> run;
    private final TaskListener listener;

    public JnksLogPollingProviderServiceImpl(JobsService jobsService, Run<?, ?> run, TaskListener listener, boolean verbose) {
        this.jobsService = jobsService;
        this.run = run;
        this.listener = listener;
        this.log = listener.getLogger();
        this.verbose = verbose;
    }

    @Override
    public String pollJobLog(String jobGuid) throws JobServiceException {
        JobExecutionDto jobExecutionDto = jobsService.pollAndWaitForJobFinished(jobGuid, this::callbackFunction, verbose);

        if (jobExecutionDto.getState() != JobState.COMPLETED) {
            listener.error(AddVersionBuilder_AddVersion_error_jobFailure(jobExecutionDto.getState().toString()));
            run.setResult(Result.FAILURE);
            return null;
        } else {
            log.println(AddVersionBuilder_AddVersion_success_analysisComplete());
            run.setResult(Result.SUCCESS);
            return jobExecutionDto.getGuid();
        }
    }

    private JobExecutionDto callbackFunction(JobExecutionDto jobExecutionDto) {
        log.println(JobsSteps_changed(JobStepTranslationHelper.getStepTranslation(jobExecutionDto.getCurrentStep())));
        return jobExecutionDto;
    }
}
