package com.castsoftware.aip.console.tools.core.dto.export;

import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobStepType;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class JobExecutionDto {
    private String guid;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private JobType jobType;
    private JobStepType currentStep;
    private String startedBy;
    private Map<String, String> jobParameters = new HashMap<>();
    private List<JobStepExecutionDto> jobStepExecutions = new ArrayList<>();
    private JobState state = JobState.STARTING;
    private LocalDateTime startTime = null;
    private LocalDateTime endTime = null;
}
