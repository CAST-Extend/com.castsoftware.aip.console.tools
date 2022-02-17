package com.castsoftware.aip.console.tools.core.dto.export;

import com.castsoftware.aip.console.tools.core.dto.jobs.JobStepType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class JobStepExecutionDto {
    private JobStepType stepType;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String logPath;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

