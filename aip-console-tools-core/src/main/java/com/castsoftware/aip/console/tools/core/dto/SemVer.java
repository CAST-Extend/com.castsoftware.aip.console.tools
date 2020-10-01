package com.castsoftware.aip.console.tools.core.dto;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@Builder
public class SemVer implements Comparable<SemVer> {
    private static final Pattern AIPCONSOLE_VERSION_PATTERN = Pattern.compile("(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)-?(?<add>.*)");

    private int major;
    private int minor;
    private int patch;
    private String additional;

    public static SemVer parse(String semverInput) {
        SemVerBuilder semVer = SemVer.builder();
        Matcher matcher = AIPCONSOLE_VERSION_PATTERN.matcher(semverInput);
        if (matcher.matches()) {
            semVer.major(Integer.parseInt(matcher.group("major"), 10));
            semVer.minor(Integer.parseInt(matcher.group("minor"), 10));
            semVer.patch(Integer.parseInt(matcher.group("patch"), 10));
            semVer.additional(matcher.group("add"));
        }
        return semVer.build();
    }

    @Override
    public int compareTo(SemVer o) {
        int result = Integer.compare(major, o.major);
        if (result != 0) {
            return result;
        }
        result = Integer.compare(minor, o.minor);
        if (result != 0) {
            return result;
        }
        result = Integer.compare(patch, o.patch);
        if (result != 0) {
            return result;
        }
        return StringUtils.compare(additional, o.additional);
    }
}
