package com.castsoftware.aip.console.tools.core.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;

public final class DateUtils {
    public static final String JSON_DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final DateTimeFormatter JSON_DATE_FORMAT = DateTimeFormatter.ofPattern(JSON_DATE_FORMAT_PATTERN)
            .withZone(ZoneOffset.UTC);

    private DateUtils() {
    }

    public static LocalDateTime parseJsonLocalDateTime(String string) {
        return ZonedDateTime.parse(string, JSON_DATE_FORMAT)
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public static String toJsonString(LocalDateTime date) {
        return toZonedDateTime(date).format(JSON_DATE_FORMAT);
    }

    public static ZonedDateTime toZonedDateTime(LocalDateTime date) {
        return ZonedDateTime.of(date, ZoneId.systemDefault());
    }

    public static LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public static LocalDateTime toLocalDateTime(long epochMilli) {
        return toLocalDateTime(Instant.ofEpochMilli(epochMilli));
    }
}
