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
    private String token;

    private Session(List<ApiConnection.GameVersion> gameVersions, String token) {
        this.gameVersions = gameVersions;
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

        if (logger.isDebugEnabled()) {
            List<String> games = gamesVersions.stream()
                .map(g -> "%s (%s)".formatted(g.slug(), g.version()))
                .toList();
            logger.debug("Game slugs found: {}", games);
        }

        return new Session(gamesVersions, connection.getToken());
    }


    public List<String> getSlugs() {
        return gameVersions.stream().map(ApiConnection.GameVersion::slug).toList();
    }

    public ApiConnection.GameVersion getGameVersion(String slug) {
        final ApiConnection.GameVersion gameVersion = gameVersions.stream()
            .filter(p -> p.slug().equals(slug))
            .findFirst()
            .orElse(null);
        if (gameVersion == null) {
            panic("Slug '%s' not found, available slugs: %s".formatted(slug, getSlugs()));
        }
        return gameVersion;
    }

    public ManifestHandler getManifestHandler(ApiConnection.GameVersion gameVersion, Path downloadsCache) {
        logger.info("Processing Game Version: {}@{}", gameVersion.slug(), gameVersion.version());
        return new ManifestHandler(downloadManifest(gameVersion, downloadsCache));
    }

    private Path downloadManifest(ApiConnection.GameVersion gameVersion, Path downloadsCache) {
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

    public String getToken() {
        return token;
    }
}
