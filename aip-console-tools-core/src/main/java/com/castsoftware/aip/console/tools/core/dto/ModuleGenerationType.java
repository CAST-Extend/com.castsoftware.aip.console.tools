package com.castsoftware.aip.console.tools.core.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;

import java.util.EnumSet;
import java.util.stream.Collectors;

public enum ModuleGenerationType {
    ONE_PER_TECHNO,
    ONE_PER_AU,
    FULL_CONTENT;

    @JsonCreator
    public static ModuleGenerationType fromString(String value) {
        return value == null || StringUtils.equalsIgnoreCase(value, "USE_DEFAULT") ? null : ModuleGenerationType.valueOf(value.toUpperCase());
    }

    @Override
    @JsonValue
    public String toString() {
        return name().toLowerCase();
    }

    public static boolean exists(String value) {
        try {
            return fromString(value) != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String getAllowed(ModuleGenerationType exceptThis) {
        return EnumSet.complementOf(EnumSet.of(exceptThis)).stream().map(ModuleGenerationType::toString).collect(Collectors.joining("; "));
    }
}
