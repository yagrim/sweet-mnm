package org.mnm.cache;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.tools.FileUtils;

import static org.mnm.tools.FileUtils.humanReadableSize;

// Deletes the old cache of versions before 1.8.0. These versions kept a "downloads" directory
// inside the path of each client. Now all clients share one global cache (see CacheHandler).
// TODO eventually remove, when we feel it's been enough time or v2.0.0
class LegacyCacheHandler {

    private static final Logger logger = LoggerFactory.getLogger(LegacyCacheHandler.class);

    CacheCleaner.Result deleteLegacyDownloads(Collection<Path> clientPaths) {
        // Multiple clients can share the same base path
        final Set<Path> paths = clientPaths
            .stream()
            .map(path -> path.toAbsolutePath().resolve("downloads"))
            .collect(Collectors.toSet());

        CacheCleaner.Result result = CacheCleaner.Result.EMPTY;
        for (Path legacyPath : paths) {
            result = result.plus(deleteAll(legacyPath));
        }
        return result;
    }

    private static CacheCleaner.Result deleteAll(Path folder) {
        if (!Files.isDirectory(folder)) {
            return CacheCleaner.Result.EMPTY;
        }
        final int files;
        final long size;
        try (Stream<Path> stream = Files.walk(folder)) {
            final List<Path> regular = stream.filter(Files::isRegularFile).toList();
            files = regular.size();
            size = regular.stream().mapToLong(LegacyCacheHandler::sizeOf).sum();
            FileUtils.deleteFolder(folder);
            Files.delete(folder);
        } catch (IOException | UncheckedIOException e) {
            logger.warn("Could not delete old cache folder {}: {}", folder, e.getMessage());
            return CacheCleaner.Result.EMPTY;
        }
        logger.info("Deleted old cache folder {}: {} files, freed {}", folder, files, humanReadableSize(size));
        return new CacheCleaner.Result(files, size);
    }

    private static long sizeOf(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0;
        }
    }
}
