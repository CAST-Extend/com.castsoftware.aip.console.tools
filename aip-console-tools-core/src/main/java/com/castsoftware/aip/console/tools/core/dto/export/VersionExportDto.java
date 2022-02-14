package com.castsoftware.aip.console.tools.core.dto.export;

import com.castsoftware.aip.console.tools.core.utils.VersionObjective;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class VersionExportDto {

    private String guid;
    private Set<String> ignorePatterns = new HashSet<>();
    private Set<String> exclusionRules = new HashSet<>();
    private Set<VersionObjective> objectives = new HashSet<>();
    private String sourcePath;
    List<AnalysisReportItemDto> analysisReport;
}

