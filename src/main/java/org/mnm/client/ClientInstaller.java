package org.mnm.client;

import org.mnm.api.Session;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.Environment;
import org.mnm.manifest.Manifest;
import org.mnm.manifest.ManifestHandler;
import org.mnm.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.mnm.config.Client.Status.*;
import static org.mnm.tools.FileUtils.fileExists;
import static org.mnm.tools.ProcessUtils.panic;
import static org.mnm.tools.UrlBuilder.buildUrl;

/**
 * Installs or repairs the installation.
 */
public class ClientInstaller {

    private static final Logger logger = LoggerFactory.getLogger(ClientInstaller.class);

    private final ConfigDb configDb;

    public ClientInstaller(ConfigDb configDb) {
        this.configDb = configDb;
    }

    public InstallationResult install(InstallOptions options,
                                      Path workDir, String apiBaseUrl) {

        Client currentClient;
        Session session;

        if (!StringUtils.isEmpty(options.slug())) {
            final String slug = options.slug();
            currentClient = configDb.getClient(slug);
            if (currentClient == null) {
                panic("No client found: run 'install' command first.");
            }
            var sessions = configDb.getSessions(slug);
            if (sessions.isEmpty()) {
                panic("No session found: run 'install' command first.");
            }
            logger.debug("Found {} sessions for '{}'", sessions.size(), slug);
            session = Session.login(sessions.get(0).token(), apiBaseUrl);
        } else {
            session = Session.login(options.username(), options.password(), apiBaseUrl);
            currentClient = configDb.getClient(session.getSlug());
        }

        final String slug = session.getSlug();
        final Installation installation = new Installation(workDir, slug);
        final String installPath = installation.getInstallPath().toString();
        if (currentClient == null) {
            Client client = new Client(slug, session.getVersion(), INSTALLING, installPath);
            configDb.addClient(client);
            configDb.addSession(new org.mnm.config.Session(session.getSlug(), session.getToken()));
        } else {
            configDb.updateClient(slug, session.getVersion(), REPAIRING, installPath);
        }

        final ManifestHandler manifestHandler = session.getManifestHandler();

        // Validate files
        final List<Manifest.File> invalid = new ArrayList<>();
        final List<Manifest.File> missing = new ArrayList<>();
        for (Manifest.File file : manifestHandler.getFiles()) {
            final Path location = installation.getInstallPath(file.path());
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
            logger.debug("Files to repair: {}", invalid.size());
            installFiles(invalid, session, installation);
        }

        // download only invalid files
        if (!missing.isEmpty()) {
            logger.info("Installing new files");
            logger.debug("Files to install: {}", missing.size());
            installFiles(missing, session, installation);
        }

        configDb.updateClient(slug, session.getVersion(), COMPLETED, installPath);

        return new InstallationResult(missing.size(), invalid.size());
    }

    record InstallationResult(int missing, int invalid) {

    }

    // We could have async workers to download and extract in parallel
    private static void installFiles(List<Manifest.File> files, Session session, Installation installation) {
        for (Manifest.File file : files) {
            FileHelper.downloadChunks(file, session.getChunksUrl(), installation);
        }
        for (Manifest.File file : files) {
            FileHelper.extract(file, installation);
        }
    }

    static class FileHelper {

        private static void downloadChunks(Manifest.File file, String chunksUrl, Installation installation) {
            for (Manifest.Bundle bundle : file.getBundlesList()) {
                final String bundleName = bundle.bundleCrc() + ".bin";
                final Path downloadPath = installation.getBundlePath(bundleName);
                System.out.println("Downloading chunks for bundle: " + downloadPath.toAbsolutePath());

                if (!fileExists(downloadPath)) {
                    Downloader.downloadFile(buildUrl(chunksUrl, bundleName).toString(), downloadPath);
                }

                String crc = HashFunctions.InMemory.crc64(downloadPath);
                if (!crc.equals(bundle.bundleCrc())) {
                    panic("CRC validation failed for: " + bundleName);
                }
            }
        }

        private static void extract(Manifest.File file, Installation installation) {
            final Path destination = installation.getInstallPath(file.path());
            FileUtils.createDirectories(destination);

            Zstd.Section[] sections = file.getBundlesList()
                    .stream()
                    .map(bundle -> new Zstd.Section(installation.getBundlePath(bundle.resolveName()), bundle.fileSectionLength()))
                    .toArray(Zstd.Section[]::new);
            Zstd.InMemory.decompress(destination, sections);
        }
    }

}
