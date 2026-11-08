package org.mnm.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.config.SettingsStore;

import static org.mnm.config.Settings.DEFAULT_CACHE_AUTO_CLEAR;
import static org.mnm.config.Settings.readCacheRetentionDays;
import static org.mnm.config.SettingsStore.CACHE_AUTO_CLEAR_KEY;
import static org.mnm.tools.FileUtils.humanReadableSize;

// TODO Move to separated package with https://github.com/yagrim/sweet-mnm/issues/131.
//      It will become something independent from the clients.
public class CacheCleaner {

    private static final Logger logger = LoggerFactory.getLogger(CacheCleaner.class);

    public record Result(int deletedFiles, long freedBytes) {

        static final Result EMPTY = new Result(0, 0);

        Result plus(Result other) {
            return new Result(deletedFiles + other.deletedFiles, freedBytes + other.freedBytes);
        }
    }

    private final SettingsStore settingsStore;
    private final Clock clock;

    public CacheCleaner(SettingsStore settingsStore) {
        this(settingsStore, Clock.systemUTC());
    }

    CacheCleaner(SettingsStore settingsStore, Clock clock) {
        this.settingsStore = settingsStore;
        this.clock = clock;
    }

    public Result clean(Collection<Path> clientPaths) {
        if (!settingsStore.getBoolean(CACHE_AUTO_CLEAR_KEY, DEFAULT_CACHE_AUTO_CLEAR)) {
            logger.debug("Cache auto-clear not active");
            return Result.EMPTY;
        }

        final int retentionDays = readCacheRetentionDays(settingsStore);
        final Instant threshold = clock.instant().minus(Duration.ofDays(retentionDays));

        Result result = Result.EMPTY;
        for (Path downloadsPath : downloadsPaths(clientPaths)) {
            result = result.plus(deleteOlderThan(downloadsPath, threshold));
        }
        logger.info("Cache auto-clear: deleted {} files older than {} days, freed {}",
            result.deletedFiles(), retentionDays, humanReadableSize(result.freedBytes()));
        return result;
    }

    // Clients can share the same base path
    private static Set<Path> downloadsPaths(Collection<Path> clientPaths) {
        final Set<Path> paths = new LinkedHashSet<>();
        for (Path clientPath : clientPaths) {
            if (clientPath != null) {
                paths.add(new Installation(clientPath, "").getDownloadsPath());
            }
        }
        return paths;
    }

    private static Result deleteOlderThan(Path downloadsPath, Instant threshold) {
        if (!Files.isDirectory(downloadsPath)) {
            return Result.EMPTY;
        }
        final List<Path> files;
        try (Stream<Path> stream = Files.walk(downloadsPath)) {
            files = stream.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            logger.warn("Could not read cache folder {}: {}", downloadsPath, e.getMessage());
            return Result.EMPTY;
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
        return new Result(deletedFiles, freedBytes);
    }
}
