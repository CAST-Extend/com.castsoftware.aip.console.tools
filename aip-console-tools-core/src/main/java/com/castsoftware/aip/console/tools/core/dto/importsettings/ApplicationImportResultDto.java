package com.castsoftware.aip.console.tools.core.dto.importsettings;

import com.castsoftware.aip.console.tools.core.dto.ObjectErrorDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplicationImportResultDto {
    private String appName;
    private boolean imported;
    private ObjectErrorDto error;
}

