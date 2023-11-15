package com.castsoftware.aip.console.tools.core.dto;

import com.castsoftware.aip.console.tools.core.dto.jobs.LogPollingProvider;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.File;

@Getter
@Setter
@Builder
public class FastScanProperties {
    String applicationName;
    File filePath;
    String exclusionPatterns;
    ExclusionRuleType[] exclusionRules;
    private long sleepDuration;

    @Builder.Default
    private boolean verbose = true;
    private String domainName;
    private LogPollingProvider logPollingProvider;
}
