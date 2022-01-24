package com.castsoftware.aip.console.tools.core.dto.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportDto {

    private SettingsExportDto settings;
    @Builder.Default
    private Collection<DomainExportDto> domains = new ArrayList<>();
    private Set<ExclusionTemplateDto> exclusionTemplates;
}

