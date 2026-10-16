package org.mnm.release;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.api.GithubConnector;
import org.mnm.config.SettingsStore;

public class ReleaseChecker {

    private static final Logger logger = LoggerFactory.getLogger(ReleaseChecker.class);

    static final String LAST_CHECK_KEY = "release.last-check";
    static final String LATEST_VERSION_KEY = "release.latest-version";

    // Unauthenticated GitHub API calls have a limit of 60 per hour
    static final Duration CHECK_INTERVAL = Duration.ofHours(12);

    private final GithubConnector githubConnector;
    private final String currentVersion;
    private final Clock clock;

    // Injecting Clock only for tests
    public ReleaseChecker(GithubConnector githubConnector, String currentVersion, Clock clock) {
        this.githubConnector = githubConnector;
        this.currentVersion = currentVersion;
        this.clock = clock;
    }

    // Returns the latest version when it is newer than the current one.
    // It never throws: a failed check must not stop the launcher.
    public Optional<String> findNewRelease(SettingsStore settingsStore) {
        try {
            final Optional<ReleaseVersion> current = ReleaseVersion.parse(currentVersion);
            if (current.isEmpty()) {
                logger.debug("Not a release version: skipping release check: {}", currentVersion);
                return Optional.empty();
            }
            return latestVersion(settingsStore)
                .filter(latest -> latest.compareTo(current.get()) > 0)
                .map(ReleaseVersion::toString);
        } catch (RuntimeException e) {
            logger.info("Could not check for new releases: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ReleaseVersion> latestVersion(SettingsStore settingsStore) {
        final Instant now = clock.instant();
        final String lastCheck = settingsStore.get(LAST_CHECK_KEY);
        if (lastCheck != null && Instant.parse(lastCheck).plus(CHECK_INTERVAL).isAfter(now)) {
            logger.debug("Using release version from last check: {}", lastCheck);
            return ReleaseVersion.parse(settingsStore.get(LATEST_VERSION_KEY));
        }

        final String latestTag = githubConnector.getLatestRelease().tagName();
        logger.debug("Latest release: {}", latestTag);
        settingsStore.put(LAST_CHECK_KEY, now.toString());
        settingsStore.put(LATEST_VERSION_KEY, latestTag);
        return ReleaseVersion.parse(latestTag);
    }
}
