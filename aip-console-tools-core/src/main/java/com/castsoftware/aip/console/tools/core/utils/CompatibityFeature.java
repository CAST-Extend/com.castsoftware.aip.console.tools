package com.castsoftware.aip.console.tools.core.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;

public enum CompatibityFeature {
    DEBUG_OPTIONS("1.24.0");
    private final String version;

    CompatibityFeature(String version) {
        this.version = version;
    }

    public String getVersionString() {
        return version;
    }

    public VersionInformation getVersion() {
        return VersionInformation.fromString(version);
    }

    public static  VersionInformation toVersion(String apiVersion){
        return VersionInformation.fromString(StringUtils.replace(apiVersion,"-","."));
    }

    @JsonCreator
    public static CompatibityFeature fromString(String value) {
        return value == null ? null : CompatibityFeature.valueOf(value.toUpperCase());
    }

    @Override
    @JsonValue
    public String toString() {
        return name().toLowerCase();
    }
}
