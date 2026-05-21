package org.mnm.config;

import java.nio.file.Path;

public record Client(
    String slug,
    String version,
    Status status,
    Path path
) {

    public enum Status {
        INSTALLING, REPAIRING, COMPLETED
    }
}
