package com.castsoftware.aip.console.tools.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DebugOptionsDto {
    @Builder.Default
    boolean showSql=false;
    @Builder.Default
    boolean activateAmtMemoryProfile=false;
    @Builder.Default
    private boolean amtMemoryProfilingLogAvailable=false;
}
