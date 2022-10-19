package com.castsoftware.aip.console.tools.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExclusionRuleDto {
    private String rule;
    @Builder.Default
    private boolean enabled = true;

    public ExclusionRuleDto(ExclusionRuleType exclusionRuleType) {
        rule = exclusionRuleType.name();
        enabled = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExclusionRuleDto)) {
            return false;
        }
        ExclusionRuleDto baseDto = (ExclusionRuleDto) o;
        return rule.equals(baseDto.getRule());
    }

    @Override
    public int hashCode() {
        return Objects.hash(rule);
    }
}
