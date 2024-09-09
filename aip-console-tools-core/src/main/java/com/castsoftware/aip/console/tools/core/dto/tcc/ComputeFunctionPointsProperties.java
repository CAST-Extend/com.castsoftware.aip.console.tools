package com.castsoftware.aip.console.tools.core.dto.tcc;

import com.castsoftware.aip.console.tools.core.dto.jobs.LogPollingProvider;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ComputeFunctionPointsProperties {
    String applicationName;
    @Builder.Default
    private boolean wait = true;

    @Builder.Default
    private boolean verbose = true;
    private LogPollingProvider logPollingProvider;
}
