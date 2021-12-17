package com.castsoftware.aip.console.tools.core.dto.export;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExtendSettingsDto {
    private String extendUrl;
    private String extendUsername;
    private String extendPassword;
    private boolean enableSendReport;
    private String extendApikey;
    private String extendMode;
    private String extendProxyMode;
}
