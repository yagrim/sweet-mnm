package org.mnm.manifest;

import org.mnm.config.Environment;
import org.mnm.tools.Downloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

public class ManifestHandler {

    private static final Logger logger = LoggerFactory.getLogger(ManifestHandler.class);

    private final List<Manifest.File> manifest;

    public ManifestHandler(Path manifestPath) {
        this.manifest = new ManifestParser().parse(manifestPath);
    }

    public ManifestHandler(String manifestUrl) {
        this.manifest = new ManifestParser().parse(downloadManifest(manifestUrl));
    }

    public List<Manifest.File> getFiles() {
        return manifest;
    }

    public Map<String, Set<String>> buildBundleIndex() {
        final Map<String, Set<String>> bundleIndex = new TreeMap<>();
        manifest.forEach(file -> {
            file.chunks().forEach(chunk -> {
                final String key = chunk.bundleCrc();
                if (bundleIndex.containsKey(key)) {
                    bundleIndex.get(key).add(file.path());
                } else {
                    Set<String> values = new HashSet<>();
                    values.add(file.path());
                    bundleIndex.put(key, values);
                }
            });
        });
        return bundleIndex;
    }

    private static Path downloadManifest(String manifestUrl) {
        Path manifest = Environment.downloads.resolve(lastSegment(manifestUrl));

        if (manifest.toFile().exists()) {
            logger.info("Skipping manifest download: {} already present", manifest.getFileName());
        } else {
            Downloader.downloadFile(manifestUrl, manifest);
        }
        return manifest;
    }

    private static String lastSegment(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    public Manifest.File findByFilePath(String path) {
        return getFiles().stream()
                .filter(file -> file.path().equals(path))
                .findFirst()
                .orElse(null);
    }
}
