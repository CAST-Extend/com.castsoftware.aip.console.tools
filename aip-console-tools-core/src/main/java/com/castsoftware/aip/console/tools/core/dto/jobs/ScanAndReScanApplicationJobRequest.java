package com.castsoftware.aip.console.tools.core.dto.jobs;

import com.castsoftware.aip.console.tools.core.dto.ModuleGenerationType;
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

    private boolean includeFastScan;

    private String sourcePath;

    private String moduleGenerationType;

    public ScanAndReScanApplicationJobRequest moduleGenerationType(ModuleGenerationType generationType) {
        if (generationType != null && (generationType != ModuleGenerationType.FULL_CONTENT)) {
            moduleGenerationType = generationType.toString();
        }
        return this;
    }

    @Override
    public String toString() {
        return "Request{" +
                "appGuid='" + appGuid + '\'' +
                ", targetNode='" + targetNode + '\'' +
                ", caipVersion='" + caipVersion + '\'' +
                ", snapshotName='" + snapshotName + '\'' +
                ", runAnalysis='" + runAnalysis + '\'' +
                ", uploadApplication='" + uploadApplication + '\'' +
                ", publishToEngineering='" + publishToEngineering + '\'' +
                ", publishToEngineering='" + publishToEngineering + '\'' +
                ", processImaging='" + processImaging + '\'' +
                '}';
    }
}
