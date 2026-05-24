package org.mnm.client;

import java.nio.file.Path;

class Installation {

    private final Path baseDir;
    private final String slug;

    Installation(Path baseDir, String slug) {
        this.baseDir = baseDir.toAbsolutePath();
        this.slug = slug;
    }

    Path getInstallPath() {
        return baseDir.resolve(slug);
    }

    Path getInstallPath(String filePath) {
        return getInstallPath().resolve(filePath.substring(1)).toAbsolutePath();
    }

    Path getDownloadsPath() {
        return baseDir.resolve("downloads");
    }

    Path getBundlesPath() {
        return getDownloadsPath().resolve("bundles");
    }

    Path getBundlePath(String name) {
        return getBundlesPath().resolve(name);
    }
}
