package com.castsoftware.aip.console.tools.core.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FeatureEnum {
    FAST_TRIPLET_INSTALLATION,
    COMBINED_EXTENSION_INSTALLATION,
    GENERATE_CONFIG_RULE,
    CHECK_ARCHITECTURE_MODEL,
    CANCEL_JOB,
    EXECUTION_REPORTS_VERSIONS,
    UNLOCK_TRIPLET,
    DISPLAY_RESULTS_SECURITY_DATAFLOW,
    USE_BLACKBOXES_FOR_SECURITY_DATAFLOW,
    SHOW_HOMEPAGE_TILES;

    @JsonCreator
    public static FeatureEnum fromString(String value) {
        return value == null ? null : FeatureEnum.valueOf(value.toUpperCase());
    }

    @Override
    @JsonValue
    public String toString() {
        return name().toLowerCase();
    }
}
