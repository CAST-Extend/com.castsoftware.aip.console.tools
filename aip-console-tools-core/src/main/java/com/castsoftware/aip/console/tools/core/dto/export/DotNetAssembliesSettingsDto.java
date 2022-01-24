package com.castsoftware.aip.console.tools.core.dto.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DotNetAssembliesSettingsDto {
    @Builder.Default
    private boolean autoCreateFromRoot = true;
    @Builder.Default
    private List<String> assemblyLocations = new ArrayList<>();
}

