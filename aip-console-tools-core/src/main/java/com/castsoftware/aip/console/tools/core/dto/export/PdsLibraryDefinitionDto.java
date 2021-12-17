package com.castsoftware.aip.console.tools.core.dto.export;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PdsLibraryDefinitionDto {
    private String libraryExtension;
    private String libraryContent;
    private String banner;
    private int leftMargin;
    private int lineMaxLength;
}

