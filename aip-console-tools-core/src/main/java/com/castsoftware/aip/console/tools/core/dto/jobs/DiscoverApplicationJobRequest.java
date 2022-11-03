package com.castsoftware.aip.console.tools.core.dto.jobs;

import com.castsoftware.aip.console.tools.core.dto.ExclusionRuleDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
public class DiscoverApplicationJobRequest {
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
    private String caipVersion;
    private String targetNode;
}
