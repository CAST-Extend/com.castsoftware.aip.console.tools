package com.castsoftware.aip.console.tools.core.dto.export;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class DomainExportDto {

    private String guid;
    private String name;
    private List<String> resourceOwners = new ArrayList<>();
    private Collection<ApplicationExportDto> applications = new ArrayList<>();

    public DomainExportDto(String name) {
        this.name = name;
    }

    public DomainExportDto(String guid, String name, List<String> resourceOwners) {
        this.guid = guid;
        this.name = name;
        this.resourceOwners = resourceOwners;
    }
}
