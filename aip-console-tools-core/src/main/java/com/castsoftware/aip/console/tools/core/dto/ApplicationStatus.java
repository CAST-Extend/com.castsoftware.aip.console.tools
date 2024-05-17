package com.castsoftware.aip.console.tools.core.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ApplicationStatus {
        IMAGING_READY,
        ERROR,
        IN_PROGRESS,
        INCOMPLETE,
        CANCELLED,
        IS_DELETED,
        PROFILING_DONE;
    @JsonCreator
    public static ApplicationStatus fromString(String value) {
        return value == null ? null : valueOf(value.toUpperCase());
    }
}
