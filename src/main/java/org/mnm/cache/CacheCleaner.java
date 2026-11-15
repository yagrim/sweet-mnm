package org.mnm.cache;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mnm.tools.FileUtils.humanReadableSize;

// Runs the automatic cleanup of the downloads cache at startup.
// It decides what to delete: the old client-relative "downloads" directories, and the files
// of the global cache that are older than the retention period.
// It does not do file work. LegacyCacheHandler deletes the old directories. CacheHandler owns
// the global cache and its path.
public class CacheCleaner {

    private static final Logger logger = LoggerFactory.getLogger(CacheCleaner.class);

    public record Result(int deletedFiles, long freedBytes) {

        static final Result EMPTY = new Result(0, 0);

        Result plus(Result other) {
            return new Result(deletedFiles + other.deletedFiles, freedBytes + other.freedBytes);
        }
    }

    private final CacheHandler cacheHandler;
    private final LegacyCacheHandler legacyCacheHandler = new LegacyCacheHandler();
    private final Clock clock;

    public CacheCleaner(CacheHandler cacheHandler) {
        this(cacheHandler, Clock.systemUTC());
    }

    CacheCleaner(CacheHandler cacheHandler, Clock clock) {
        this.cacheHandler = cacheHandler;
        this.clock = clock;
    }

    /**
     * Deletes the downloads cache.
     * <p>
     * First, deletes the client-relative "downloads" directory of each client.
     * Versions before 1.8.0 used this path for the cache. This step always runs,
     * also when auto-clear is not active.
     * <p>
     * Then, if auto-clear is active, it deletes the files in the global cache that are older
     * than the retention period. Manifest files are kept.
     *
     * @param clientPaths the paths of the clients. Clients can share a path. Null elements are ignored.
     * @return the number of deleted files and the freed bytes, for both steps together
     */
    public Result clean(Collection<Path> clientPaths) {
        Result result = legacyCacheHandler.deleteLegacyDownloads(clientPaths);
        if (!cacheHandler.isAutoClear()) {
            logger.debug("Cache auto-clear not active");
            return result;
        }

        final int retentionDays = cacheHandler.getRetentionDays();
        final Instant threshold = clock.instant().minus(Duration.ofDays(retentionDays));

        result = result.plus(cacheHandler.deleteOlderThan(threshold));
        logger.info("Cache auto-clear: deleted {} files older than {} days, freed {}",
            result.deletedFiles(), retentionDays, humanReadableSize(result.freedBytes()));
        return result;
    }
}
