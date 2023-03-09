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
    private String snapshotName;

    @Builder.Default
    private boolean runAnalysis = false;
    @Builder.Default
    private boolean uploadApplication = false;
    @Builder.Default
    private boolean processImaging = false;
    @Builder.Default
    private boolean publishToEngineering = false;

    private String moduleGenerationType;

    @Override
    public String toString() {
        return "Request{" +
                "appGuid='" + appGuid + '\'' +
                ", targetNode='" + targetNode + '\'' +
                ", caipVersion='" + caipVersion + '\'' +
                ", snapshotName='" + snapshotName + '\'' +
                ", moduleGenerationType='" + moduleGenerationType + '\'' +
                '}';
    }
}
