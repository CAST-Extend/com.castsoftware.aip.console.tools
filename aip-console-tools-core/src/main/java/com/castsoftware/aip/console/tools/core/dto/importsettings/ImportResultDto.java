package com.castsoftware.aip.console.tools.core.dto.importsettings;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;

@Getter
@Setter
public class ImportResultDto {

    private Collection<DomainImportResultDto> domains = new ArrayList<>();
}
