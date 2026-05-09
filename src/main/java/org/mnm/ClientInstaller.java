package org.mnm;

import org.mnm.config.Environment;
import org.mnm.manifest.Manifest;
import org.mnm.manifest.ManifestHandler;
import org.mnm.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.mnm.config.Environment.API_BASE_URL;
import static org.mnm.tools.FileUtils.fileExists;
import static org.mnm.tools.ProcessUtils.panic;
import static org.mnm.tools.UrlBuilder.buildUrl;

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

        Session session = Session.login(username, password, API_BASE_URL);
        ManifestHandler manifestHandler = session.getManifestHandler();
        List<Manifest.File> files = manifestHandler.getFiles();

        Set<Integer> sizes = new TreeSet<>();
        files.stream().forEach(f -> sizes.add(f.chunks().size()));

        // Validate files
        final List<Manifest.File> invalid = new ArrayList<>();
        final List<Manifest.File> missing = new ArrayList<>();
        for (Manifest.File file : files) {
            final Path location = FileHelper.getLocation(file, session.getSlug());
            if (!location.toFile().exists()) {
                missing.add(file);
            } else {
                if (location.toFile().length() != file.totalSize()) {
                    invalid.add(file);
                } else {
                    final String calculatedCrc = HashFunctions.OS.xxh3(location);
                    if (!calculatedCrc.equals(file.fileHash())) {
                        invalid.add(file);
                    }
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
            installFiles(invalid, session);
        }

        // download only invalid files
        if (!missing.isEmpty()) {
            logger.info("Installing new files");
            installFiles(missing, session);
        }

    }

    // We could have async workers to download and extract in parallel
    private static void installFiles(List<Manifest.File> files, Session session) {
        for (Manifest.File file : files) {
            FileHelper.downloadChunks(file, session.getChunksUrl());
        }
        for (Manifest.File file : files) {
            FileHelper.extract(file, session.getSlug());
        }
    }

    static class FileHelper {

        private static void downloadChunks(Manifest.File file, String chunksUrl) {
            for (Manifest.Bundle bundle : file.getBundlesList()) {
                final String bundleName = bundle.bundleCrc() + ".bin";
                final Path downloadPath = Environment.chunks.resolve(bundleName);
                System.out.println("Downloading chunks for bundle: " + downloadPath.toAbsolutePath());

                if (!fileExists(downloadPath)) {
                    Downloader.downloadFile(buildUrl(chunksUrl, bundleName).toString(), downloadPath);
                }
            }
        }

        private static void extract(Manifest.File file, String slug) {
            final Path destination = getLocation(file, slug);
            FileUtils.createDirectories(destination);

            Zstd.Section[] sections = file.getBundlesList()
                    .stream()
                    .map(bundle -> new Zstd.Section(Environment.chunks.resolve(bundle.resolveName()), bundle.fileSectionLength()))
                    .toArray(Zstd.Section[]::new);
            Zstd.InMemory.decompress(destination, sections);
        }

        private static Path getLocation(Manifest.File file, String slug) {
            return Environment.client.resolve(slug).resolve(file.path().substring(1));
        }
    }

}
