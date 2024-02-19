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

    public static String getFileName(String filename) {
        int lastBackslashIndex = filename.lastIndexOf("\\");
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > lastBackslashIndex && dotIndex < filename.length() - 1) {
            return filename.substring(lastBackslashIndex + 1, dotIndex);
        } else {
            return filename.substring(lastBackslashIndex + 1);
        }
    }
}
