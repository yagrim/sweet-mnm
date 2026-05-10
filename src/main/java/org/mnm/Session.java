package org.mnm;

import org.mnm.api.ApiConnection;
import org.mnm.api.ApiConnector;
import org.mnm.api.RestClient;
import org.mnm.config.Environment;
import org.mnm.manifest.ManifestHandler;
import org.mnm.tools.Downloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

import static org.mnm.tools.FileUtils.fileExists;
import static org.mnm.tools.ProcessUtils.panic;
import static org.mnm.tools.StringUtils.isEmpty;

public class Session {

    private static final Logger logger = LoggerFactory.getLogger(Session.class);

    private final ApiConnection.GameVersion gameVersion;

    private Session(ApiConnection.GameVersion gameVersion) {
        this.gameVersion = gameVersion;
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

        List<ApiConnection.GameVersion> gamesVersions = connection.getGamesVersions();
        if (gamesVersions.isEmpty()) {
            panic("No game versions found");
        }
        if (gamesVersions.size() > 1) {
            panic("Too many game versions found");
        }

        return new Session(gamesVersions.get(0));
    }

    public ManifestHandler getManifestHandler() {
        logger.info("Processing Game Version: {}@{}", gameVersion.slug(), gameVersion.version());
        return new ManifestHandler(downloadManifest());
    }

    private Path downloadManifest() {
        String manifestUrl = gameVersion.manifest_url();
        String manifestName = getLastSegment(manifestUrl);
        final Path downloadPath = Environment.downloads.resolve(manifestName);
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
}
