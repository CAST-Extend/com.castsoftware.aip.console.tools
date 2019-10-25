package com.castsoftware.aip.console.tools.core.dto.jobs;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeJobStateRequest {
    private JobState state;
}
