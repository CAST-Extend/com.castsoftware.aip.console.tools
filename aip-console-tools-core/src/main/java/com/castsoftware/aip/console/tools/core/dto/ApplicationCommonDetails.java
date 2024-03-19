package com.castsoftware.aip.console.tools.core.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
public class ApplicationCommonDetails {

    @Builder.Default
    public List<ApplicationCommonDetailsDto> applicationCommonDetailsDtoSet = new ArrayList<>();
}
