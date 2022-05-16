package com.castsoftware.aip.console.tools.core.dto;

import com.castsoftware.aip.console.tools.core.dto.jobs.DeliveryPackageDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryConfigurationDto {
    private String guid;
    private Set<String> ignorePatterns;
    private Set<ExclusionRuleType> exclusionRules;
    private Set<DeliveryPackageDto> packages;
}
