package com.castsoftware.aip.console.tools.core.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VersionStatus {
    OPENED,
    PURGED,
    DELIVERING,
    DELIVERED,
    ACCEPTED,
    ANALYSING,
    ANALYSIS_DONE,
    ANALYSIS_FAILED,
    SNAPSHOT_TAKEN,
    SNAPSHOT_DONE,
    VALIDATED;

    @JsonCreator
    public static VersionStatus fromString(String value) {
        return value == null ? null : VersionStatus.valueOf(value.toUpperCase());
    }

    public static VersionStatus[] analysedStatuses() {
        return new VersionStatus[]{ANALYSIS_DONE, SNAPSHOT_TAKEN, SNAPSHOT_DONE, VALIDATED};
    }

    @Override
    @JsonValue
    public String toString() {
        return name().toLowerCase();
    }

    public static VersionStatus[] fromStrings(String... arr) {
        if (arr == null || arr.length == 0) {
            return new VersionStatus[0];
        }
        VersionStatus[] result = new VersionStatus[arr.length];
        for (int i = 0; i < arr.length; i++) {
            result[i] = fromString(arr[i]);
        }
        return result;
    }
}
