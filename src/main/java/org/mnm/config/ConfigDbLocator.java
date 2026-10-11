package org.mnm.config;

import java.nio.file.Path;
import java.util.function.Supplier;

import static org.mnm.config.Environment.getSweetConfigLocation;

public class ConfigDbLocator implements Supplier<Path> {

    @Override
    public Path get() {
        return getSweetConfigLocation()
            .resolve("sweet-config.db");
    }
}
