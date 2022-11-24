package com.castsoftware.aip.console.tools.core.dto;

import com.castsoftware.aip.console.tools.core.utils.VersionObjective;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Getter
@Setter
public class ApplicationOnboardingDto {
    private String sourcePath;
    private boolean discoveryDone;
    private boolean configurationDone;
    private long nbFiles;
    private long applicationSize;
    private long nbLoc;
    private String status;
    private String versionGuid;
    private String versionName;
    private String deliveryConfigGuid;
    private String guid;
    private LocalDateTime createdDate;
    private long estimatedTime;
    private long estimatedAnalysisTime;
    private long estimatedSnapshotTime;
    private String estimatedWeight;
    private String targetNode;
    private String databaseName;
    private String caipVersion;
    private Set<VersionObjective> objectives = EnumSet.noneOf(VersionObjective.class);
}
