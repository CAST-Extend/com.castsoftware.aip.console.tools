package com.castsoftware.aip.console.tools.core.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class DateUtils {

    private DateUtils() {
    }

    public static LocalDateTime parseJsonLocalDateTime(String string) {
        return ZonedDateTime.parse(string, DateFormatter.JSON_DATE_FORMAT)
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public static String toJsonString(LocalDateTime date) {
        return toZonedDateTime(date).format(DateFormatter.JSON_DATE_FORMAT);
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

