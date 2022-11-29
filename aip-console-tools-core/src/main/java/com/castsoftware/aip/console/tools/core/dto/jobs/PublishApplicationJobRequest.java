package com.castsoftware.aip.console.tools.core.dto.jobs;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PublishApplicationJobRequest {
    private String appGuid;
    @Builder.Default
    private boolean processImaging = true;
    @Builder.Default
    private boolean uploadApplication = false;
    @Builder.Default
    private boolean backupApplication = false;
}
