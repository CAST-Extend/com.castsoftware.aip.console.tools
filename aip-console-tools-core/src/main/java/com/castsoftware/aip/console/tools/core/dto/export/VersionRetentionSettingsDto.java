package com.castsoftware.aip.console.tools.core.dto.export;


import lombok.Data;

@Data
public class VersionRetentionSettingsDto {
    private boolean numberVersionsEnabled;
    private int numberVersions;
    private boolean numberVersionsMaxEnabled;
    private int numberVersionsMax;
    private boolean numberDaysVersionRetentionEnabled;
    private int numberDaysVersionRetention;
    private boolean deleteVersionSnapshots;
}

