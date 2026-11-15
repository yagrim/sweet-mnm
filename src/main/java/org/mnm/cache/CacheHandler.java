package org.mnm.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.config.Environment;
import org.mnm.config.SettingsStore;
import org.mnm.tools.FileUtils;

// Owns the global downloads cache and its options.
// It keeps the path of the cache and does the file operations on it: get the size, delete
// all files, and delete the files older than a date.
// It also reads and writes the cache options (auto-clear and retention days) in the settings store.
// CacheCleaner decides when to clean. This class does the work.
public class CacheHandler {

    private static final Logger logger = LoggerFactory.getLogger(CacheHandler.class);

    private static final String AUTO_CLEAR_KEY = "cache.auto-clear";
    private static final String RETENTION_DAYS_KEY = "cache.retention-days";

    private static final String MANIFEST_EXTENSION = ".manifest";

    private static final boolean DEFAULT_AUTO_CLEAR = true;
    private static final int DEFAULT_RETENTION_DAYS = 10;

    public static final List<Integer> RETENTION_DAYS = List.of(5, 10, 15, 30);

    private final SettingsStore settingsStore;
    private final Path downloadsPath;

    public CacheHandler(SettingsStore settingsStore) {
        this(settingsStore, Environment.getDownloadsLocation());
    }

    public CacheHandler(SettingsStore settingsStore, Path downloadsPath) {
        this.settingsStore = settingsStore;
        this.downloadsPath = downloadsPath;
    }

    public long getSize() {
        return FileUtils.getFolderSize(downloadsPath);
    }

    // Deletes all files, including the manifest files
    public void clear() throws IOException {
        FileUtils.deleteFolder(downloadsPath);
    }

    // Manifest files are kept. Only clear() deletes them.
    CacheCleaner.Result deleteOlderThan(Instant threshold) {
        if (!Files.isDirectory(downloadsPath)) {
            return CacheCleaner.Result.EMPTY;
        }
        final List<Path> files;
        try (Stream<Path> stream = Files.walk(downloadsPath)) {
            files = stream
                .filter(Files::isRegularFile)
                .filter(file -> !isManifest(file))
                .toList();
        } catch (IOException e) {
            logger.warn("Could not read cache folder {}: {}", downloadsPath, e.getMessage());
            return CacheCleaner.Result.EMPTY;
        }

        int deletedFiles = 0;
        long freedBytes = 0;
        for (Path file : files) {
            try {
                BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
                if (attributes.lastModifiedTime().toInstant().isBefore(threshold)) {
                    Files.delete(file);
                    deletedFiles++;
                    freedBytes += attributes.size();
                }
            } catch (IOException e) {
                logger.debug("Could not delete cache file {}: {}", file, e.getMessage());
            }
        }
        return new CacheCleaner.Result(deletedFiles, freedBytes);
    }

    private static boolean isManifest(Path file) {
        return file.getFileName().toString().endsWith(MANIFEST_EXTENSION);
    }

    // Returns true when the options were not set and the defaults were written.
    // Returns false when the options were already set.
    public boolean initDefaults() {
        if (settingsStore.get(AUTO_CLEAR_KEY) != null) {
            return false;
        }
        settingsStore.putBoolean(AUTO_CLEAR_KEY, DEFAULT_AUTO_CLEAR);
        if (settingsStore.get(RETENTION_DAYS_KEY) == null) {
            settingsStore.putInt(RETENTION_DAYS_KEY, DEFAULT_RETENTION_DAYS);
        }
        logger.debug("Cache auto-clear options not found. Default options set.");
        return true;
    }

    public boolean isAutoClear() {
        return settingsStore.getBoolean(AUTO_CLEAR_KEY, DEFAULT_AUTO_CLEAR);
    }

    public void saveAutoClear(boolean value) {
        settingsStore.putBoolean(AUTO_CLEAR_KEY, value);
    }

    // TODO refactor, should be moved to CacheHandler based on responsibilities?
    public int getRetentionDays() {
        try {
            int candidate = settingsStore.getInt(RETENTION_DAYS_KEY, DEFAULT_RETENTION_DAYS);
            if (RETENTION_DAYS.contains(candidate)) {
                return candidate;
            }
            logger.debug("Invalid cache retention days found in db {}", candidate);
        } catch (NumberFormatException e) {
            logger.debug("Invalid cache retention days found in db: {}", e.getMessage());
        }
        return DEFAULT_RETENTION_DAYS;
    }

    public void saveRetentionDays(int days) {
        settingsStore.putInt(RETENTION_DAYS_KEY, days);
    }
}
