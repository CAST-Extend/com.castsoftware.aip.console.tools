package com.castsoftware.aip.console.tools.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LogUtils {
    private static final String HOST_PORT_REGEXP = "\\s*[_']?\\s*(?<sensitiveInfo>\\S+:\\d+)";
    public static final String REPLACEMENT_STR = "****";

    private static List<Pattern> SENSITIVE_INFO_PATTERNS = new ArrayList<>();
    static{
        SENSITIVE_INFO_PATTERNS.add(Pattern.compile("\\*\\*\\s*HOST:\\s*(?<sensitiveInfo>\\S+)"));
        SENSITIVE_INFO_PATTERNS.add(Pattern.compile("\\*\\*\\s*PORT:\\s*(?<sensitiveInfo>\\d+)"));
        SENSITIVE_INFO_PATTERNS.add(Pattern.compile("\\-{2}apiKey[:|\\=]?\\s*(?<sensitiveInfo>\\S+)"));
        SENSITIVE_INFO_PATTERNS.add(Pattern.compile("(CastStorageService" + HOST_PORT_REGEXP + ")"));
        SENSITIVE_INFO_PATTERNS.add(Pattern.compile("____Connected to" + HOST_PORT_REGEXP + " database"));
        SENSITIVE_INFO_PATTERNS.add(Pattern.compile("Server" + HOST_PORT_REGEXP + " on CastStorageService'"));
        SENSITIVE_INFO_PATTERNS.add(Pattern.compile("(-logFilePath|Log file path):\\s*(?<sensitiveInfo>\\S+)"));
        SENSITIVE_INFO_PATTERNS.add(Pattern.compile("-licenseKey\\s*:\\s*(?<sensitiveInfo>.+)"));
    }

    public static String replaceAllSensitiveInformation(String toMath) {
        Optional<Matcher> m = SENSITIVE_INFO_PATTERNS.stream().parallel().map(pat -> pat.matcher(toMath)).filter(match -> match.find()).findFirst();
        if (m.isPresent()) {
            String what = m.get().group("sensitiveInfo");
            return toMath.replace(what, REPLACEMENT_STR);
        }
        return toMath;
    }
}
