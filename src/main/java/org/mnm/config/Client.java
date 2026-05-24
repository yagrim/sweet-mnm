package org.mnm.config;

import java.nio.file.Path;

/**
 * Stored metadata for an installed MnM client.
 *
 * @param slug    client slug
 * @param version installed client version
 * @param status  current installation status
 * @param path    absolute path to the installation working directory (does not include slug)
 */
public record Client(
    String slug,
    String version,
    Status status,
    Path path
) {

    /**
     * Installation state tracked for a client record.
     */
    public enum Status {
        INSTALLING, REPAIRING, COMPLETED
    }
}
