package com.castsoftware.aip.console.tools.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class ApplicationDto {
    private String guid;
    private String name;
    private String state;
    private boolean inPlaceMode;
    private String caipVersion;
    private String targetNode;
    private String schemaPrefix;

    private String imagingTenant;
    private boolean onboarded;
    private boolean onboardedNotAnalyzed;
    private boolean managed;
    private VersionDto version;
}
