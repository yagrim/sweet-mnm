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

/**
 * Deletes the old cache of versions before 1.8.0. These versions kept a "downloads" directory
 * inside the path of each client. Only the known content is deleted ("bundles" folder and
 * ".manifest" files). The "downloads" folder itself and any other content stay.
 * Now all clients share one global cache (see {@link CacheHandler}).
 */
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
            result = result.plus(deleteKnownContent(legacyPath));
        }
        return result;
    }

    // The "downloads" folder can be a folder of the user (for example, on a case-insensitive
    // system with the client in the home directory, it can be the OS "Downloads" folder).
    // Only delete the content that old versions created. Never delete the folder itself.
    private static CacheCleaner.Result deleteKnownContent(Path folder) {
        if (!Files.isDirectory(folder)) {
            return CacheCleaner.Result.EMPTY;
        }
        CacheCleaner.Result result = deleteBundles(folder.resolve("bundles"));
        try (Stream<Path> stream = Files.list(folder)) {
            final List<Path> manifests = stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".manifest"))
                .toList();
            for (Path manifest : manifests) {
                result = result.plus(deleteFile(manifest));
            }
        } catch (IOException | UncheckedIOException e) {
            logger.warn("Could not read old cache folder {}: {}", folder, e.getMessage());
        }
        return result;
    }

    private static CacheCleaner.Result deleteBundles(Path bundles) {
        if (!Files.isDirectory(bundles)) {
            return CacheCleaner.Result.EMPTY;
        }
        final int files;
        final long size;
        try (Stream<Path> stream = Files.walk(bundles)) {
            final List<Path> regular = stream.filter(Files::isRegularFile).toList();
            files = regular.size();
            size = regular.stream().mapToLong(LegacyCacheHandler::sizeOf).sum();
            FileUtils.deleteFolder(bundles);
            Files.delete(bundles);
        } catch (IOException | UncheckedIOException e) {
            logger.warn("Could not delete old cache folder {}: {}", bundles, e.getMessage());
            return CacheCleaner.Result.EMPTY;
        }
        logger.info("Deleted old cache folder {}: {} files, freed {}", bundles, files, humanReadableSize(size));
        return new CacheCleaner.Result(files, size);
    }

    private static CacheCleaner.Result deleteFile(Path file) {
        final long size = sizeOf(file);
        try {
            Files.delete(file);
        } catch (IOException e) {
            logger.warn("Could not delete old cache file {}: {}", file, e.getMessage());
            return CacheCleaner.Result.EMPTY;
        }
        logger.info("Deleted old cache file {}, freed {}", file, humanReadableSize(size));
        return new CacheCleaner.Result(1, size);
    }

    private static long sizeOf(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0;
        }
    }
}
