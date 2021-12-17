package com.castsoftware.aip.console.tools.core.dto.export;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ExtensionSettingsDto {

    private String versionRange;
    private boolean whitelistEnabled;
    private boolean forceInstallEnabled;
    private boolean autoUpdateEnabled;
    private Set<AllowedExtensionDto> whitelist = new HashSet<>();
}
