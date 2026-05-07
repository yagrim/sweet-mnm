package org.mnm;

import org.mnm.config.Environment;
import org.mnm.manifest.Manifest;
import org.mnm.manifest.ManifestHandler;
import org.mnm.tools.FileUtils;
import org.mnm.tools.HashFunctions;
import org.mnm.tools.StringUtils;
import org.mnm.tools.Zstd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.mnm.tools.ProcessUtils.panic;

/**
 * Installs or repairs the installation.
 */
// TODO test
public class ClientInstaller {

    private static final Logger logger = LoggerFactory.getLogger(ClientInstaller.class);

    public void install(String username, String password) {

        if (StringUtils.isEmpty(username)) {
            panic("Missing parameter: '--username'");
        }
        if (StringUtils.isEmpty(password)) {
            panic("Missing parameter: '--password'");
        }

        ManifestService manifestService = ManifestService.login(username, password);
        ManifestHandler manifestHandler = manifestService.getManifestHandler();
        List<Manifest.File> files = manifestHandler.getFiles();

        Set<Integer> sizes = new TreeSet<>();
        files.stream().forEach(f -> sizes.add(f.chunks().size()));

        // Validate files
        final List<Manifest.File> invalid = new ArrayList<>();
        final List<Manifest.File> missing = new ArrayList<>();
        for (Manifest.File file : files) {
            final Path location = FileHelper.getLocation(file);
            if (!location.toFile().exists()) {
                missing.add(file);
            } else {
                final String calculatedCrc = HashFunctions.OS.xxh3(location);
                if (!calculatedCrc.equals(file.fileHash())) {
                    invalid.add(file);
                }
            }
        }

        if (!invalid.isEmpty()) {
            logger.info("Found {} invalid file(s)", invalid.size());
            invalid.stream().forEach(s -> logger.info(s.path()));
        }

        if (!missing.isEmpty()) {
            logger.info("Found {} missing file(s)", missing.size());
            missing.stream().forEach(s -> logger.info(s.path()));
        }

        // download only invalid files
        if (!invalid.isEmpty()) {
            logger.info("Installing modified files");
            installFiles(invalid, manifestService);
        }

        // download only invalid files
        if (!missing.isEmpty()) {
            logger.info("Installing new files");
            installFiles(missing, manifestService);
        }

    }

    // We could have async workers to download and extract in parallel
    private void installFiles(List<Manifest.File> files, ManifestService manifestService) {
        for (Manifest.File file : files) {
            FileHelper.downloadChunks(file, manifestService);
        }
        for (Manifest.File file : files) {
            FileHelper.extract(file);
        }
    }

    static class FileHelper {

        private static Path getLocation(Manifest.File file) {
            return Environment.mnm.resolve(file.path().substring(1));
        }

        private static void downloadChunks(Manifest.File file, ManifestService manifestService) {
            for (Manifest.Bundle bundle : file.getBundlesList()) {
                manifestService.downloadChunk(bundle.bundleCrc());
            }
        }

        private static void extract(Manifest.File file) {
            final Path destination = getLocation(file);
            FileUtils.createDirectories(destination);

            Zstd.Section[] sections = file.getBundlesList()
                    .stream()
                    .map(bundle -> new Zstd.Section(Environment.chunks.resolve(bundle.resolveName()), bundle.fileSectionLength()))
                    .toArray(Zstd.Section[]::new);
            Zstd.InMemory.decompress(destination, sections);
        }
    }

}
