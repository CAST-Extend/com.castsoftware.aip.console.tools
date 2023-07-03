package com.castsoftware.aip.console.tools.core.dto.architecturestudio;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Category {
    MODEL,
    CUSTOM_TEMPLATE,
    STANDARD_TEMPLATE,
    CUSTOM_SET,
    CUSTOM_LAYER,
    STANDARD_SET,
    STANDARD_LAYER;

    @JsonValue
    @Override
    public String toString() {
        return name().toLowerCase();
    }

    /**
     * @return whether the supplied category is the one of an Architecture
     * Checker standard template, or of a standard library set or layer
     */
    public static boolean isStandard(Category category) {
        return (category == STANDARD_TEMPLATE || category == STANDARD_SET || category == STANDARD_LAYER);
    }

    public static boolean isLibraryComponentCategory(Category category) {
        return (category == CUSTOM_SET || category == CUSTOM_LAYER || category == STANDARD_SET || category == STANDARD_LAYER);
    }
}
