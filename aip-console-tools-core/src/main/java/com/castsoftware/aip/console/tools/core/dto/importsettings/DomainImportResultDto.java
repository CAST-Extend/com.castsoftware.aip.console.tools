package com.castsoftware.aip.console.tools.core.dto.importsettings;

import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

@Getter
@Setter
public class DomainImportResultDto {

    private String name;
    private Collection<ApplicationImportResultDto> applications;
}
