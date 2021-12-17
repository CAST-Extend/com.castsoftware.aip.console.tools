package com.castsoftware.aip.console.tools.core.dto.export;

import lombok.Data;

@Data
public class JobHistoryRetentionSettingsDto {
    private boolean numberJobsPerAppEnabled;
    private int numberJobsPerApp;
}
