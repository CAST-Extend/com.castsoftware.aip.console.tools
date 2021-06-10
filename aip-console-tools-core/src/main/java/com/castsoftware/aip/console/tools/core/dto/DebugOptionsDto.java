package com.castsoftware.aip.console.tools.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DebugOptionsDto {
    boolean showSql;
    boolean activateAmtMemoryProfile;
    private boolean amtMemoryProfilingLogAvailable;
}
