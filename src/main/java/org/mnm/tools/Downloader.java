package org.mnm.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.mnm.tools.FileUtils.humanReadableSize;

public class Downloader {

    private static final Logger logger = LoggerFactory.getLogger(Downloader.class);

    private Downloader() {
    }

    public static void downloadFile(String url, Path destination) {
        try {

            InputStream in = new URL(sanitize(url)).openStream();
            FileUtils.createDirectories(destination);
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);

            if (destination.toFile().exists() &&  destination.toFile().length() > 0) {
                logger.info("Downloaded {} to {} ({})", url, destination, humanReadableSize(destination.toFile().length()));
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
        // TODO test
        if (manifestUrl.startsWith("http://localhost"))
            return manifestUrl;

        if (manifestUrl.startsWith("http:")) {
            return "https:" + manifestUrl.substring(5);
        }
        return manifestUrl;
    }

}
