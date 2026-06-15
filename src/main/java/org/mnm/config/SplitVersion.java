package org.mnm.config;

import org.mnm.tools.StringUtils;

public final class SplitVersion {

    private final String prefix;
    private final String semver;
    private final String sha;

    SplitVersion(String fullVersion) {
        String[] parts = fullVersion.split("-", 3);
        this.prefix = parts[0];
        this.semver = parts[1];
        this.sha = parts[2];
        if (StringUtils.isEmpty(prefix) && StringUtils.isEmpty(semver) && StringUtils.isEmpty(sha)) {
            throw new IllegalArgumentException("Empty components for: " + fullVersion);
        }
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSemver() {
        return semver;
    }

    public String getSha() {
        return sha;
    }

    public String getShortSha() {
        return sha.substring(0, 7);
    }
}
