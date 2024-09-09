package com.castsoftware.aip.console.tools.core.dto.tcc;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class CheckRuleContentRequest {
    private String ruleId;
    private String ruleType;
}