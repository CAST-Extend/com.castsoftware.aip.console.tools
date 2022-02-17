package com.castsoftware.aip.console.tools.core.dto.export;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SnapshotExportIndicatorDto {
    private String id;
    private String name;
    private String description;
    private String type;
    private double value;
    private String justification;
    private String fileName;
    private String connectionType;
}
