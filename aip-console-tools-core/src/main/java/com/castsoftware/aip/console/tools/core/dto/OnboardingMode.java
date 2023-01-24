package com.castsoftware.aip.console.tools.core.dto;

public enum OnboardingMode {
    FAST_SCAN, DEEP_ANALYSIS;

    public static OnboardingMode fromString(String value) {
        return value == null ? null : OnboardingMode.valueOf(value.toUpperCase());
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }

}
