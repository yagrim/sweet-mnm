package org.mnm;

import org.mnm.api.ApiConnection;
import org.mnm.api.ApiConnector;
import org.mnm.config.Environment;
import org.mnm.config.Factories;
import org.mnm.manifest.ManifestHandler;
import org.mnm.tools.Downloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.mnm.tools.ProcessUtils.panic;
import static org.mnm.tools.StringUtils.isEmpty;

public class ManifestService {

    private static final Logger logger = LoggerFactory.getLogger(ManifestService.class);

    private final ApiConnection.GameVersion gameVersion;

    private ManifestService(ApiConnection.GameVersion gameVersion) {
        this.gameVersion = gameVersion;
    }

    public static ManifestService login(String username, String password) {
        System.out.println("Connecting with " + username + "...");
        if (isEmpty(username) || isEmpty(password)) {
            panic("Username or password is empty");
        }

        ApiConnector apiConnector = Factories.apiConnector();
        ApiConnection connection = apiConnector.getConnection(username, password);

        List<ApiConnection.GameVersion> gamesVersions = connection.getGamesVersions();
        if (gamesVersions.isEmpty()) {
            panic("No game versions found");
        }
        if (gamesVersions.size() > 1) {
            panic("Too many game versions found");
        }

        return new ManifestService(gamesVersions.get(0));
    }

    public ManifestHandler getManifestHandler() {
        logger.info("Processing Game Version: {}@{}", gameVersion.slug(), gameVersion.version());
        return new ManifestHandler(downloadManifest());
    }

    private Path downloadManifest() {
        String manifestUrl = gameVersion.manifest_url();
        String manifestName = getLastSegment(manifestUrl);
        final Path downloadPath = Environment.downloads.resolve(manifestName);
        if (!fileExists(downloadPath)) {
            Downloader.downloadFile(manifestUrl, downloadPath);
        }
        return downloadPath;
    }

    public static String getLastSegment(String url) {
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        int lastSlash = url.lastIndexOf('/');
        return (lastSlash >= 0) ? url.substring(lastSlash + 1) : url;
    }

    public Path downloadChunk(String bundleCrc) {
        final String bundleName = bundleCrc + ".bin";
        String url = gameVersion.chunksUrl() + "/" + bundleName;

        final Path downloadPath = Environment.chunks.resolve(bundleName);
        if (!fileExists(downloadPath)) {
            Downloader.downloadFile(url, downloadPath);
        }
        return downloadPath;
    }

    private static boolean fileExists(Path downloadPath) {
        File file = downloadPath.toFile();
        return file.exists() && file.length() > 0;
    }

}
