package org.mnm.config;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Environment {

    public static final Path downloads = Path.of(".").resolve("downloads");
    public static final Path chunks = downloads.resolve("chunks");
    public static final Path mnm = downloads.resolve("mnm");

    public static Path launcherDb = Paths.get(System.getProperty("user.home"))
            .resolve(".local/share")
            .resolve("com.monstersandmemories.launcher")
            .resolve("launcher.db");
}
