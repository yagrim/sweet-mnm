package org.mnm.config;

import java.nio.file.Path;

public class Environment {

    public static final Path downloads = Path.of(".").resolve("downloads");
    public static final Path chunks = downloads.resolve("chunks");
    public static final Path mnm = downloads.resolve("mnm");

}
