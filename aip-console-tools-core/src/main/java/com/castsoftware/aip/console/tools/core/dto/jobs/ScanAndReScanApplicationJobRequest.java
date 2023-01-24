package com.castsoftware.aip.console.tools.core.dto.jobs;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ScanAndReScanApplicationJobRequest {
    protected String appGuid;
    protected String targetNode;
    protected String caipVersion;

    @Builder.Default
    private boolean runAnalysis = false;
    @Builder.Default
    private boolean uploadApplication = false;
    @Builder.Default
    private boolean processImaging = false;
    @Builder.Default
    private boolean publishToEngineering = false;
}
