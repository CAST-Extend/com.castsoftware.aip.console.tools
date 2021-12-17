package com.castsoftware.aip.console.tools.core.dto.export;


import lombok.Data;

@Data
public class SnapshotRetentionSettingsDto {
    private boolean numberSnapshotsEnabled;
    private int numberSnapshots;
    private boolean numberSnapshotsMaxEnabled;
    private int numberSnapshotsMax;
    private boolean numberDaysSnapshotRetentionEnabled;
    private int numberDaysSnapshotRetention;
}

