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

    public SplitVersion getSplitVersion() {
        return new SplitVersion(version);
    }

    /**
     * Installation state tracked for a client record.
     * - ~ING: are explicit status change due to install/repair operations.
     * - the rest: are implicit status changes due to login and version check.
     */
    public enum Status {
        NOT_INSTALLED,
        UPDATED,
        NEEDS_UPDATE,
        INSTALLING,
        REPAIRING;

        public boolean isInProgress() {
            return equals(INSTALLING) || equals(REPAIRING);
        }
    }
}
