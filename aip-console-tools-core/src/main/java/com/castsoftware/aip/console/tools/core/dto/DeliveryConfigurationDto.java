package com.castsoftware.aip.console.tools.core.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class DeliveryConfigurationDto {

    private Set<String> ignorePatterns;
    private Set packages;
}
