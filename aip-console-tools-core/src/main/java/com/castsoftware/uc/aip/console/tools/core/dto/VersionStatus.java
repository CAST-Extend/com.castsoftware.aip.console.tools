package com.castsoftware.uc.aip.console.tools.core.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author Kamil Janowski (KJA)
 */
public enum VersionStatus {
    OPENED("delivery.StatusOpened", "StatusOpened"),
    AWAITING_VALIDATION("delivery.StatusAwaitingValidation", "StatusAwaitingValidation"),
    READY_FOR_ANALYSIS("delivery.StatusReadyForAnalysis", "StatusReadyForAnalysis"),
    READY_FOR_ANALYSIS_AND_DEPLOYED("delivery.StatusReadyForAnalysisAndDeployed", "StatusReadyForAnalysisAndDeployed"),
    PURGED("delivery.StatusPurged", "StatusPurged");

    public static class InvalidVersionStatusMdaNameException extends RuntimeException {

        protected InvalidVersionStatusMdaNameException(String mdaName) {
            super("The name '" + mdaName + "' could not be resolved into a version status");
        }
    }

    private String mdaName;
    private String dbValue;

    VersionStatus(String mdaName, String dbValue) {
        this.mdaName = mdaName;
        this.dbValue = dbValue;
    }

    @JsonCreator
    public static VersionStatus fromString(String value) {
        return value == null ? null : VersionStatus.valueOf(value.toUpperCase());
    }

    public String getMdaName() {
        return mdaName;
    }

    public String getDbName() {
        return mdaName.substring("delivery.".length());
    }

    @Override
    @JsonValue
    public String toString() {
        return name().toLowerCase();
    }

    /**
     * Gets the right status based on the MDA name
     *
     * @param mdaName mda name
     * @return the version status matching the mda name
     * @throws InvalidVersionStatusMdaNameException if no status matches the specified mda name
     */
    public static VersionStatus getVersionStatusByMdaName(String mdaName) {
        for (VersionStatus status : values()) {
            if (status.mdaName.equals(mdaName)) {
                return status;
            }
        }
        throw new InvalidVersionStatusMdaNameException(mdaName);
    }

    public static VersionStatus fromDbValue(String dbValue) {
        for (VersionStatus status : values()) {
            if (status.dbValue.equals(dbValue)) {
                return status;
            }
        }
        return null;
    }
}
