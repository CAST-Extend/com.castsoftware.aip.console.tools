package com.castsoftware.aip.console.tools.core.dto.architecturestudio;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ArchitectureModelDto {

    private String guid;
    private String modelName;
    private String path;
    private int metricId;
    private int weight;
    private boolean critical;
    private String description;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;

}
