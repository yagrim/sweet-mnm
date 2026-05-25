package org.mnm.config;

import java.nio.file.Path;
import java.util.Locale;

public final class OS {

    private OS() {
    }

    public static boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase(Locale.ROOT).startsWith("windows");
    }

    public static Path getWorkingDirectory() {
        return Path.of(System.getProperty("user.dir"));
    }
}
