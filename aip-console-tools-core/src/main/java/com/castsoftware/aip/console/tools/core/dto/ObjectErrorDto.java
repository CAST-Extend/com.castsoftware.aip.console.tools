package com.castsoftware.aip.console.tools.core.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class ObjectErrorDto {
    private String code;
    private String defaultMessage;
    private Map<String, Object> arguments = new HashMap<>();
}

