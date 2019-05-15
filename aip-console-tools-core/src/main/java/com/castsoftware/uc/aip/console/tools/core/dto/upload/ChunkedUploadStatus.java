package com.castsoftware.uc.aip.console.tools.core.dto.upload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.EnumSet;

public enum ChunkedUploadStatus {
    CREATED,
    UPLOADING,
    UPLOADED,
    UPLOAD_FAILED,
    EXTRACTING,
    EXTRACTED,
    EXTRACT_FAILED;

    public static EnumSet getInProgressStatuses() {
        return EnumSet.of(UPLOADING, EXTRACTING);
    }

    @JsonCreator
    public static ChunkedUploadStatus fromString(String value) {
        return value == null ? null : ChunkedUploadStatus.valueOf(value.toUpperCase());
    }

    @JsonValue
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
