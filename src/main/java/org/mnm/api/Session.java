package org.mnm.api;

import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.manifest.ManifestHandler;
import org.mnm.tools.Downloader;

import static org.mnm.tools.FileUtils.fileExists;
import static org.mnm.tools.ProcessUtils.panic;
import static org.mnm.tools.StringUtils.isEmpty;

public class Session {

    private static final Logger logger = LoggerFactory.getLogger(Session.class);

    private final List<ApiConnection.GameVersion> gameVersions;
    private final ApiConnection.GameVersion gameVersion;
    private String token;

    private Session(List<ApiConnection.GameVersion> gameVersions, ApiConnection.GameVersion gameVersion, String token) {
        this.gameVersions = gameVersions;
        this.gameVersion = gameVersion;
        this.token = token;
    }

    public static Session login(String username, String password, ApiConnector apiConnector, TokenSupplier tokenSupplier) {
        logger.info("Authenticating with account: {}", mask(username));
        if (isEmpty(username) || isEmpty(password)) {
            panic("Username or password is empty");
        }

        return buildSession(apiConnector.login(username, password, tokenSupplier));
    }

    public static Session login(String token, ApiConnector apiConnector) {
        logger.info("Authenticating with token ...");
        if (isEmpty(token)) {
            panic("Token is empty");
        }

        return buildSession(apiConnector.login(token));
    }

    private static String mask(String email) {
        if (email == null || !email.contains("@")) return email;

        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];

        String maskedName = name.length() > 3
            ? name.substring(0, 3) + "***"
            : name + "***";

        String maskedDomain = domain.length() > 3
            ? domain.substring(0, 3) + "***"
            : domain + "***";

        return maskedName + "@" + maskedDomain;
    }

    private static Session buildSession(ApiConnection connection) {
        List<ApiConnection.GameVersion> gamesVersions = connection.getGamesVersions();
        if (gamesVersions.isEmpty()) {
            panic("No game versions found");
        }
        final ApiConnection.GameVersion defaultVersion = gamesVersions.get(0);
        if (gamesVersions.size() > 1) {
            logger.info("Multiple game slugs found: {}. Using '{}' by default, use --slug to select a different one",
                gamesVersions.stream().map(p -> p.slug()).toList(), defaultVersion.slug());
        }

        return new Session(gamesVersions, defaultVersion, connection.getToken());
    }

    /**
     * Returns a new Session with the same token, for the given slug.
     */
    public Session select(String slug) {
        final ApiConnection.GameVersion selected = gameVersions.stream()
            .filter(p -> p.slug().equals(slug))
            .findFirst()
            .orElse(null);
        if (selected == null) {
            panic("Slug '%s' not found, available slugs: %s".formatted(slug, getSlugs()));
        }
        return new Session(gameVersions, selected, token);
    }

    public List<String> getSlugs() {
        return gameVersions.stream().map(ApiConnection.GameVersion::slug).toList();
    }

    public ManifestHandler getManifestHandler(Path downloadsCache) {
        logger.info("Processing Game Version: {}@{}", gameVersion.slug(), gameVersion.version());
        return new ManifestHandler(downloadManifest(downloadsCache));
    }

    private Path downloadManifest(Path downloadsCache) {
        String manifestUrl = gameVersion.manifest_url();
        String manifestName = getLastSegment(manifestUrl);
        final Path downloadPath = downloadsCache.resolve(manifestName);
        if (fileExists(downloadPath)) {
            logger.debug("Skipping manifest download: {} already present", manifestName);
        } else {
            logger.debug("Downloading manifest: {}", manifestName);
            Downloader.downloadFile(manifestUrl, downloadPath);
        }
        return downloadPath;
    }

    private static String getLastSegment(String url) {
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        int lastSlash = url.lastIndexOf('/');
        return (lastSlash >= 0) ? url.substring(lastSlash + 1) : url;
    }

    public String getSlug() {
        return gameVersion.slug();
    }

    public String getChunksUrl() {
        return gameVersion.chunksUrl();
    }

    public String getVersion() {
        return gameVersion.version();
    }

    public String getToken() {
        return token;
    }
}
