package com.castsoftware.aip.console.tools.core.dto.export;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SnapshotDetailsDto {
    private long linesOfCode;
    private int validTransactionCount;
    private int invalidTransactionCount;
    private String enhancementMeasure;
    private int afpCount;
    private int aepCount;
    private int efpCount;
    private boolean architectureChecked;
    private boolean securityDataflowChecked;
}
