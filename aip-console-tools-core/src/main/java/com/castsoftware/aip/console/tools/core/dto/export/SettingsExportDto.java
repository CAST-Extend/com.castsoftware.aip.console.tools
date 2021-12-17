package com.castsoftware.aip.console.tools.core.dto.export;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SettingsExportDto {

    private String license;
    private DatabaseConnectionSettingsDto measurementDatabase;
    private String measurementSchemaName;
    private String sourcesFolder;
    private ExtensionSettingsDto extensionSettings = new ExtensionSettingsDto();
    private ImagingSettingsDto imagingSettings = new ImagingSettingsDto();
    private boolean validateSnapshot;
    private ExtendSettingsDto extendSettings = new ExtendSettingsDto();
    private ProxySettingsDto proxySettings = new ProxySettingsDto();
    private List<MavenRepositoryDto> mavenRepositories = new ArrayList<>();
    private BackupRetentionSettingsDto backupRetentionSettings = new BackupRetentionSettingsDto();
    private SnapshotRetentionSettingsDto snapshotRetentionSettings = new SnapshotRetentionSettingsDto();
    private VersionRetentionSettingsDto versionRetentionSettings = new VersionRetentionSettingsDto();
    private JobHistoryRetentionSettingsDto jobHistoryRetentionSettings = new JobHistoryRetentionSettingsDto();
    private List<PdsLibraryDefinitionDto> pdsLibraries = new ArrayList<>();
    private DotNetAssembliesSettingsDto dotNetAssembliesSettings = new DotNetAssembliesSettingsDto();
    private StopDeliveryConditionDto stopDeliveryCondition = new StopDeliveryConditionDto();
    private String cssOptimizeType;
    private String assessmentModelStrategyType;
    private boolean enableGlobalAssessmentModel;
    private Set<AssessmentModelRecordDto> assessmentModelRecord = new HashSet<>();
    private Set<SnapshotIndicatorSettingsDto> snapshotIndicatorSettings = new HashSet<>();
}
