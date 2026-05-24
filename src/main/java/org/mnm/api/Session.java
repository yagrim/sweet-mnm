package org.mnm.api;

import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.config.Environment;
import org.mnm.manifest.ManifestHandler;
import org.mnm.tools.Downloader;

import static org.mnm.tools.FileUtils.fileExists;
import static org.mnm.tools.ProcessUtils.panic;
import static org.mnm.tools.StringUtils.isEmpty;

public class Session {

    private static final Logger logger = LoggerFactory.getLogger(Session.class);

    private final ApiConnection.GameVersion gameVersion;
    private String token;

    private Session(ApiConnection.GameVersion gameVersion, String token) {
        this.gameVersion = gameVersion;
        this.token = token;
    }

    public static Session login(String username, String password, String baseUrl) {
        System.out.println("Connecting with " + username + "...");
        if (isEmpty(username) || isEmpty(password)) {
            panic("Username or password is empty");
        }
        if (isEmpty(baseUrl)) {
            panic("Base URL is empty");
        }

        ApiConnector apiConnector = new ApiConnector(new RestClient(baseUrl));
        ApiConnection connection = apiConnector.login(username, password);

        return buildSession(connection);
    }

    public static Session login(String token, String baseUrl) {
        System.out.println("Connecting with token ...");
        if (isEmpty(token)) {
            panic("Token is empty");
        }

        ApiConnector apiConnector = new ApiConnector(new RestClient(baseUrl));
        ApiConnection connection = apiConnector.login(token);

        return buildSession(connection);
    }

    private static Session buildSession(ApiConnection connection) {
        List<ApiConnection.GameVersion> gamesVersions = connection.getGamesVersions();
        if (gamesVersions.isEmpty()) {
            panic("No game versions found");
        }
        if (gamesVersions.size() > 1) {
            panic("Too many game versions found");
        }

        return new Session(gamesVersions.get(0), connection.getToken());
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
            logger.info("Skipping manifest download: {} already present", manifestName);
        } else {
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
