package com.castsoftware.aip.console.tools.core.dto.export;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssessmentModelRecordDto {
    private String guid;
    private String settings_guid;
    private Integer technicalCriterionId;
    private String technicalCriterionName;
    private Integer qualityRuleId;
    private String qualityRuleName;
    private Integer weight;
    private Boolean criticality;
    private Boolean enabled;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
}

