package com.castsoftware.aip.console.tools.core.dto.export;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class SnapshotExportDto {
    private String guid;
    private String name;
    private String title;
    private LocalDateTime captureDate;
    Set<SnapshotDetailsDto> details;
    Set<SnapshotExportIndicatorDto> snapshotIndicators;
}
