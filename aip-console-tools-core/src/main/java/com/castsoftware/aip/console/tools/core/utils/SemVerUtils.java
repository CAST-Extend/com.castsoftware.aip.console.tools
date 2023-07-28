package com.castsoftware.aip.console.tools.core.utils;

import com.castsoftware.aip.console.tools.core.dto.SemVer;

public class SemVerUtils {
    private SemVerUtils() {
    }

    public static VersionInformation getMinCompatibleVersion() {
        return VersionInformation.fromVersionString("2.8.3-funcrel");
    }

    public static boolean isNewerThan115(SemVer semVer) {
        return semVer != null && SemVer.builder()
                .major(1)
                .minor(15)
                .patch(Integer.MAX_VALUE)
                .build()
                .compareTo(semVer) < 0;
    }

    public static SemVer latest1_15() {
        return SemVer.builder()
                .major(1)
                .minor(15)
                .patch(Integer.MAX_VALUE)
                .build();
    }
}
