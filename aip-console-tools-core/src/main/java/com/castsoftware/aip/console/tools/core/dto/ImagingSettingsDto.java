package com.castsoftware.aip.console.tools.core.dto;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
public class ImagingSettingsDto {
    private String url;
    private String apiKey;

    public boolean isValid() {
        return !StringUtils.isAnyEmpty(url, apiKey);
    }
}
