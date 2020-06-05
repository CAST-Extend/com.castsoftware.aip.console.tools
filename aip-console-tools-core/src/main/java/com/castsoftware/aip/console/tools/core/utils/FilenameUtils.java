package com.castsoftware.aip.console.tools.core.utils;

import org.apache.commons.lang3.StringUtils;

public class FilenameUtils {
    private FilenameUtils() {
        // NOP
    }

    public static String getFileExtension(String filename) {
        if (StringUtils.endsWithIgnoreCase(filename, ".tar.gz")) {
            return "tar.gz";
        }
        return org.apache.commons.io.FilenameUtils.getExtension(filename);
    }
}
