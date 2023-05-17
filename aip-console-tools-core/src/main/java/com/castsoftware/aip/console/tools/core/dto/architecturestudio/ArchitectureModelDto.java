package com.castsoftware.aip.console.tools.core.dto.architecturestudio;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    private String name;
    private String fileName;
    private Category category;
    private Boolean valid;

}
