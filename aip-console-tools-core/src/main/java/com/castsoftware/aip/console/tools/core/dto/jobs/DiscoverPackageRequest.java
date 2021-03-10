package com.castsoftware.aip.console.tools.core.dto.jobs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiscoverPackageRequest {
    private String previousVersionGuid;
    private String sourcePath;
}
