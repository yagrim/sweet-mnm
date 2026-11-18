package org.mnm.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mnm.tools.FileUtils.humanReadableSize;

public class Downloader {

    private static final Logger logger = LoggerFactory.getLogger(Downloader.class);

    // The read timeout applies to each read, not to the full download, so large files are not affected
    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private Downloader() {
    }

    public static void downloadFile(String url, Path destination) {
        downloadFile(url, destination, DEFAULT_TIMEOUT);
    }

    static void downloadFile(String url, Path destination, Duration timeout) {
        try {

            URLConnection connection = new URL(sanitize(url)).openConnection();
            connection.setConnectTimeout((int) timeout.toMillis());
            connection.setReadTimeout((int) timeout.toMillis());
            try (InputStream in = connection.getInputStream()) {
                FileUtils.createDirectories(destination);
                Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            if (destination.toFile().exists() && destination.toFile().length() > 0) {
                logger.debug("Downloaded {} to {} ({})", url, destination, humanReadableSize(destination.toFile().length()));
            } else {
                logger.info("Could not download {}", url);
                destination.toFile().delete();
                throw new RuntimeException("Could not download " + url);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // TODO this is a workaround, we should handle http redirect
    private static String sanitize(String manifestUrl) {
        if (manifestUrl.startsWith("http://localhost")) {
            return manifestUrl;
        }
        if (manifestUrl.startsWith("http:")) {
            return "https:" + manifestUrl.substring(5);
        }
        return manifestUrl;
    }

}
