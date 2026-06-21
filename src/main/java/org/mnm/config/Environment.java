package org.mnm.config;

import java.nio.file.Path;

import org.mnm.tools.FileUtils;

public class Environment {

    public static final String API_BASE_URL = "https://account.monstersandmemories.com/api/";

    public static final boolean NATIVE_IMAGE = Boolean.parseBoolean(System.getProperty("sweet_native"));

    public static Path launcherDb = getHomeDir()
        .resolve(".local/share")
        .resolve("com.monstersandmemories.launcher")
        .resolve("launcher.db");

    public static Path getHomeDir() {
        return Path.of(System.getProperty("user.home"));
    }

    public static Path getWorkDir() {
        return Path.of(System.getProperty("user.dir")).toAbsolutePath();
    }

    public static VersionDetails versionDetails() {
        String[] split = FileUtils.readFromClasspath("version.txt").split("\n");
        return new VersionDetails(split[0], split[1]);
    }

}
