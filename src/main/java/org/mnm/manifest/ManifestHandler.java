package org.mnm.manifest;

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

    public Manifest.File findByFilePath(String path) {
        return getFiles().stream()
                .filter(file -> file.path().equals(path))
                .findFirst()
                .orElse(null);
    }
}
