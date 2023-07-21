package com.castsoftware.aip.console.tools.core.dto.jobs;

import com.sun.istack.internal.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class ApplicationJobRequest {

    @NotNull
    private String appGuid;

    private String caipVersion;

}
