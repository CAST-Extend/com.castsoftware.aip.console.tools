package com.castsoftware.aip.console.tools.core.dto.jobs;

import com.castsoftware.aip.console.tools.core.dto.ExclusionRuleDto;
import com.castsoftware.aip.console.tools.core.dto.ModuleGenerationType;
import com.castsoftware.aip.console.tools.core.utils.VersionObjective;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
public class FirstScanApplicationJobRequest {
    private final String appGuid;
    private final String sourcePath;
    private final String deliveryConfigGuid;
    private final String onboardGuid;

    @Builder.Default
    private Set<String> ignorePatterns = new HashSet<>();
    @Builder.Default
    private Set<ExclusionRuleDto> exclusionRules = new HashSet<>();
    private final String versionName;
    private final boolean deleteVersion;

    @Builder.Default
    private Set<VersionObjective> objectives = EnumSet.noneOf(VersionObjective.class);
    private String cssGuid;
    private String targetNode;
    private String caipVersion;

    @Builder.Default
    private boolean extensionAutoConfigEnabled = true;
    @Builder.Default
    private String snapshotName = "FirstScan-" + LocalDateTime.now().format(JobRequestBuilder.DELIVERY_DATETIME_FORMAT);
    @Builder.Default
    private LocalDateTime snapshotCaptureDate = LocalDateTime.now();
    @Builder.Default
    private String moduleGenerationType = ModuleGenerationType.ONE_PER_TECHNO.toString();
    @Builder.Default
    private boolean uploadApplication = false;
    @Builder.Default
    private boolean processImaging = false;
    @Builder.Default
    private boolean saveSourceFolderZip = true;
    @Builder.Default
    private boolean runAnalysis = false;
    @Builder.Default
    private boolean publishToEngineering = false;
    @Builder.Default
    private boolean backup = false;

}
