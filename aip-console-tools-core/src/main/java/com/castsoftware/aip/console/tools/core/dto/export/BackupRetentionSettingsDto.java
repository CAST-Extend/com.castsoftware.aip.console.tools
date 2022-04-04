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
public class BackupRetentionSettingsDto {
    private boolean numberDaysRetentionEnabled;
    private boolean numberBackupsEnabled;
    private int numberDaysRetention;
    private int numberBackups;
}

