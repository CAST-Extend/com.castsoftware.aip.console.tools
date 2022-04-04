package com.castsoftware.aip.console.tools.core.dto.export;

import com.castsoftware.aip.console.tools.core.dto.BaseDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ExclusionTemplateDto extends BaseDto {
    private String name;
    private Set<String> ignorePatterns = new HashSet<>();
    private boolean readOnly;
}

