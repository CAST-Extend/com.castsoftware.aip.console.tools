package com.castsoftware.aip.console.tools.core.dto.export;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SnapshotIndicatorSettingsDto {
    private String guid;
    private String id;
    private String category;
    private String name;
    private boolean enabled;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
}

