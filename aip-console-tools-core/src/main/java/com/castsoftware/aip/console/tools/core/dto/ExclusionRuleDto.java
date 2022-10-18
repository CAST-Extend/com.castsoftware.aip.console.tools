package com.castsoftware.aip.console.tools.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExclusionRuleDto {
    private String rule;
    @Builder.Default
    private boolean enabled = true;

    public ExclusionRuleDto(ExclusionRuleType exclusionRuleType) {
        rule = exclusionRuleType.name();
        enabled = true;
    }
}
