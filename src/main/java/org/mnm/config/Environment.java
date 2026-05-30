package org.mnm.config;

import java.nio.file.Path;

public class Environment {

    public static final String API_BASE_URL = "https://account.monstersandmemories.com/api/";

    public static final boolean NATIVE_IMAGE = Boolean.valueOf(System.getProperty("sweet_native"));

    public static Path launcherDb = getWorkDir()
        .resolve(".local/share")
        .resolve("com.monstersandmemories.launcher")
        .resolve("launcher.db");

    public static Path getWorkDir() {
        return Path.of(System.getProperty("user.home"));
    }

}
