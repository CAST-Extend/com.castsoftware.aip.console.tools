package com.castsoftware.aip.console.tools.core.dto;

import com.castsoftware.aip.console.tools.core.dto.jobs.LogPollingProvider;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DeepAnalyzeProperties {
    private String applicationName;
    private String snapshotName;
    private boolean processImaging;
    private boolean publishToEngineering;
    @Builder.Default
    private ModuleGenerationType moduleGenerationType = ModuleGenerationType.FULL_CONTENT;
    private long sleepDuration;
    @Builder.Default
    private boolean verbose = true;
    private LogPollingProvider logPollingProvider;
}
