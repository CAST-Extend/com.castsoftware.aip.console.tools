package com.castsoftware.aip.console.tools.core.dto;

import java.util.EnumSet;
import java.util.stream.Collectors;

public enum ModuleGenerationType {
    ONE_PER_TECHNO,
    ONE_PER_AU,
    FULL_CONTENT;

    public static ModuleGenerationType fromString(String value) {
        return value == null ? null : ModuleGenerationType.valueOf(value.toUpperCase());
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    public static String getAllowed(ModuleGenerationType exceptThis) {
        return EnumSet.complementOf(EnumSet.of(exceptThis)).stream().map(ModuleGenerationType::toString).collect(Collectors.joining("; "));
    }
}

