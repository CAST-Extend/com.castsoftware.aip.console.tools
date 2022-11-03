package com.castsoftware.aip.console.tools.core.dto.jobs;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FirstScanApplicationJobRequest {
    private final String appGuid;
    private String targetNode;
    private String caipVersion;
}
