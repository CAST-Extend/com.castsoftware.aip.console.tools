package com.castsoftware.aip.console.tools.core.utils;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class DateFormatter {

    /**
     * Date Time format for DMT CLI and Snapshot capture Date
     */
    public static final DateTimeFormatter DELIVERY_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    public static final DateTimeFormatter DMT_ENTITY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DMT_ENTITY_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final DateTimeFormatter CAST_MS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter LOG_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    // Any json dateFormat modification should be carefully tested, cause it can affect SynchronisationService functionality
    public static final String JSON_DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final DateTimeFormatter JSON_DATE_FORMAT = DateTimeFormatter.ofPattern(JSON_DATE_FORMAT_PATTERN)
            .withZone(ZoneOffset.UTC);
}

