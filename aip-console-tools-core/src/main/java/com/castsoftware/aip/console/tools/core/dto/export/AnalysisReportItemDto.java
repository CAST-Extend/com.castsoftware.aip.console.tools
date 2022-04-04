package com.castsoftware.aip.console.tools.core.dto.export;

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
public class AnalysisReportItemDto {
    private SourceFileInfoDto fileInfo;
    private int nbFoundFiles;
    private int nbExcludedFiles;
    private int nbProcessedFiles;
    private int nbAnalyzedFiles;
    private int nbUnanalyzedFiles;
    private int nbSkippedFiles;
    private int nbPartiallyAnalyzedFiles;
    private int nbUnresolvedFiles;
}
