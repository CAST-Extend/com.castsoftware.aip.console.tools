package com.castsoftware.aip.console.tools.core.dto.jobs;

import com.castsoftware.aip.console.tools.core.dto.ExclusionRuleDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Builder
@Getter
@Setter
public class DiscoverApplicationJobRequest {
    private String appGuid;
    private String sourcePath;
    private String deliveryConfigGuid;
    private String onboardGuid;
    
    @Builder.Default
    private Set<String> ignorePatterns = new HashSet<>();
    @Builder.Default
    private Set<ExclusionRuleDto> exclusionRules = new HashSet<>();
    private String versionName;
    private boolean deleteVersion;
}
