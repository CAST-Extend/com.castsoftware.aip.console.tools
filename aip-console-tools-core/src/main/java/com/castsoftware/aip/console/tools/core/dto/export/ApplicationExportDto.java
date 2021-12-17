package com.castsoftware.aip.console.tools.core.dto.export;

import com.castsoftware.aip.console.tools.core.dto.DebugOptionsDto;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
public class ApplicationExportDto {

    private String guid;
    private String name;
    private List<String> resourceOwners = new ArrayList<>();
    private ConnectionProfileExportDto connectionProfile;
    private boolean emptyModulesIgnored = true;
    private boolean inPlaceMode;
    private Collection<VersionExportDto> versions = new ArrayList<>();
    private List<MavenRepositoryDto> mavenRepositories = new ArrayList<>();
    private DebugOptionsDto debugOptions;
}

