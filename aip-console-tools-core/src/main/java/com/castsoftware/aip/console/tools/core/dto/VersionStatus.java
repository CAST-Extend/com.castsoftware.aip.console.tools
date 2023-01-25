package com.castsoftware.aip.console.tools.core.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VersionStatus {
    OPENED,
    PURGED,
    DELIVERING,
    DELIVERED,
    DISCOVERED,
    ACCEPTED,
    UPDATING_EXTENSIONS,
    ANALYSING,
    ANALYSIS_ONGOING,
    ANALYSIS_DONE,
    ANALYSIS_FAILED,
    PREPARING_ANALYSIS_DATA,
    ANALYSIS_DATA_PREPARED,
    PROCESSING_IMAGING,
    IMAGING_PROCESSED,
    SNAPSHOT_ONGOING,
    SNAPSHOT_TAKEN,
    SNAPSHOT_DONE,
    VALIDATED,
    PUBLISHING,
    UNKNOWN,
    SCANNED,
    ANALYZED,
    FULLY_ANALYZED;

    @JsonCreator
    public static VersionStatus fromString(String value) {
        return value == null ? null : VersionStatus.valueOf(value.toUpperCase());
    }

    public static VersionStatus[] analysedStatuses() {
        return new VersionStatus[]{ANALYSIS_DONE, SNAPSHOT_DONE, SNAPSHOT_TAKEN, VALIDATED, ANALYSIS_DATA_PREPARED, IMAGING_PROCESSED};
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
