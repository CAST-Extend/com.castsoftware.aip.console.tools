package com.castsoftware.aip.console.tools.core.utils;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LogUtils {
    private static final String APIKEY_REGEXP = "\\-{2}apiKey[:|\\=]?\\s*(?<sensitiveInfo>\\S+)";
    private static final String HOST_REGEXP = "\\*\\*\\s*HOST:\\s*(?<sensitiveInfo>\\S+)";
    private static final String PORT_REGEXP = "\\*\\*\\s*PORT:\\s*(?<sensitiveInfo>\\d+)";
    private static final String HOST_PORT_REGEXP = "\\s*[_']?\\s*(?<sensitiveInfo>\\S+:\\d+)";
    private static final String CONNECTION_REGEXP_1 = "(CastStorageService" + HOST_PORT_REGEXP + ")";
    private static final String CONNECTION_REGEXP_2 = "____Connected to" + HOST_PORT_REGEXP + " database";
    private static final String CONNECTION_REGEXP_3 = "Server" + HOST_PORT_REGEXP + " on CastStorageService'";
    private static final String LOGFILE_REGEXP = "(-logFilePath|Log file path):\\s*(?<sensitiveInfo>\\S+)";
    private static final String LICENSE_REGEXP = "-licenseKey\\s*:\\s*(?<sensitiveInfo>.+)";
    public static final String REPLACEMENT_STR = "****";

    private static List<Pattern> SENSITIVE_INFO_PATTERNS = Lists.newArrayList(
            Pattern.compile(HOST_REGEXP), Pattern.compile(PORT_REGEXP),Pattern.compile(APIKEY_REGEXP),
            Pattern.compile(CONNECTION_REGEXP_1), Pattern.compile(CONNECTION_REGEXP_2), Pattern.compile(CONNECTION_REGEXP_3),
            Pattern.compile(LOGFILE_REGEXP), Pattern.compile(LICENSE_REGEXP));

    public static String replaceAllSensitiveInformation(String toMath) {
        Matcher m = SENSITIVE_INFO_PATTERNS.stream().parallel().map(pat -> pat.matcher(toMath)).filter(match -> match.find()).findFirst().orElse(null);
        if (m != null) {
            String what = m.group("sensitiveInfo");
            return toMath.replace(what, REPLACEMENT_STR);
        }
        return toMath;
    }
}
