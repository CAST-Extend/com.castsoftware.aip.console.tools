package com.castsoftware.aip.console.tools.core.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public final class FileUtils {
    public static void writeToFile(Path location, String content) {
        // If the file doesn't exist, create and write to it
        // If the file exists, truncate (remove all content) and write to it
        try (FileWriter writer = new FileWriter(location.toString());
             BufferedWriter bw = new BufferedWriter(writer)) {
            bw.write(content);
        } catch (IOException e) {
            log.error("IOException: %s%n", e);
        }
    }

    public static boolean exists(String fullFilePath) {
        try {
            return Paths.get(fullFilePath).toFile().exists();
        } catch (InvalidPathException e) {
            return false;
        }
    }
}
