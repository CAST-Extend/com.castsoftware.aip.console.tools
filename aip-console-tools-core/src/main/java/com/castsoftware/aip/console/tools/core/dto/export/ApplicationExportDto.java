package com.castsoftware.aip.console.tools.core.dto.export;

import com.castsoftware.aip.console.tools.core.dto.DebugOptionsDto;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class ApplicationExportDto {

    private String guid;
    private String name;
    private String title;

    private List<String> resourceOwners = new ArrayList<>();
    private ConnectionProfileExportDto connectionProfile;
    private boolean emptyModulesIgnored = true;
    private boolean inPlaceMode;
    private Collection<VersionExportDto> versions = new ArrayList<>();
    private List<MavenRepositoryDto> mavenRepositories = new ArrayList<>();
    private DebugOptionsDto debugOptions;
    private Set<DlmFileDto> dlmFiles = new HashSet<>();
    Set<JobExecutionDto> jobExecutions;
}

