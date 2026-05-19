package org.mnm.config;

import java.nio.file.Path;
import java.util.function.Supplier;

public class ConfigDbLocator implements Supplier<Path> {

    @Override
    public Path get() {
        final Path base = OS.isWindows()
                ? Path.of(System.getenv("LOCALAPPDATA"))
                : Path.of(System.getProperty("user.home")).resolve(".local/share");

        return base
                .resolve("com.monstersandmemories.sweet")
                .resolve("sweet-config.db");
    }
}
