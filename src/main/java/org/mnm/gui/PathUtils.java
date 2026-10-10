package org.mnm.gui;

import java.nio.file.Path;

public class PathUtils {


    static Path replaceDrive(Path path, char unit) {
        String pathText = path.toString();
        if (doesNotContainWindowsDrive(pathText)) {
            throw new IllegalArgumentException("Non-Windows path found: " + path);
        }
        return Path.of(unit + pathText.substring(1));
    }

    private static boolean doesNotContainWindowsDrive(String pathText) {
        return pathText.length() < 2 || pathText.charAt(1) != ':';
    }
}
