package com.castsoftware.aip.console.tools.core.dto.jobs;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class OnboardApplicationJobRequest {
    private String caipVersion;
    private String appLicense;
    private String appName;
    private String domainName;
}
