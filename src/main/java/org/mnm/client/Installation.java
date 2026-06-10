package org.mnm.client;

import java.nio.file.Path;

public class Installation {

    private final Path baseDir;
    private final String slug;

    public Installation(Path baseDir, String slug) {
        this.baseDir = baseDir.toAbsolutePath();
        this.slug = slug;
    }

    Path getInstallPath() {
        return baseDir.resolve(slug).toAbsolutePath();
    }

    Path getInstallPath(String filePath) {
        return getInstallPath().resolve(filePath.substring(1)).toAbsolutePath();
    }

    public Path getDownloadsPath() {
        return baseDir.resolve("downloads");
    }

    Path getBundlesPath() {
        return getDownloadsPath().resolve("bundles");
    }

    Path getBundlePath(String name) {
        return getBundlesPath().resolve(name);
    }
}
