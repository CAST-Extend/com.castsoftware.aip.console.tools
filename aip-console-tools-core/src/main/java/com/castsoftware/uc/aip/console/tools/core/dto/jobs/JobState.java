package com.castsoftware.uc.aip.console.tools.core.dto.jobs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum JobState {
    COMPLETED, STARTING, STARTED, STOPPED, FAILED, CANCELING, CANCELED;

    @JsonCreator
    public static JobState fromString(String value) {
        return value == null ? null : JobState.valueOf(value.toUpperCase());
    }

    @Override
    @JsonValue
    public String toString() {
        return name().toLowerCase();
    }

    public static JobState[] getInProgressStates() {
        return new JobState[]{STARTING, STARTED, CANCELING};
    }

    public static JobState[] getNotInProgressStates() {
        return new JobState[]{COMPLETED, STOPPED, FAILED, CANCELED};
    }
}
