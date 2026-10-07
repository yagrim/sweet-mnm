package org.mnm.client;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.api.ApiConnector;
import org.mnm.api.Session;
import org.mnm.api.TokenSupplier;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.Token;
import org.mnm.events.ClientEventHandler;
import org.mnm.events.InstallationListener;
import org.mnm.manifest.Manifest;
import org.mnm.tools.Downloader;
import org.mnm.tools.FileUtils;
import org.mnm.tools.HashFunctions;
import org.mnm.tools.JwtParser;
import org.mnm.tools.StringUtils;
import org.mnm.tools.Zstd;

import static org.mnm.config.Client.Status.UPDATED;
import static org.mnm.tools.FileUtils.fileExists;
import static org.mnm.tools.FileUtils.getAllFiles;
import static org.mnm.tools.ProcessUtils.panic;
import static org.mnm.tools.UrlBuilder.buildUrl;

/**
 * Installs or repairs the installation.
 */
public class ClientInstaller {

    private static final Logger logger = LoggerFactory.getLogger(ClientInstaller.class);


    @FunctionalInterface
    interface Installer {
        // TODO having to pass status seems a smell.
        // Instead, maybe have an OPs table to audit what was the last operation
        void install(InstallerOptions options, ConfigDb configDb, Client.Status status);
    }

    private final ConfigDb configDb;
    private final ApiConnector apiConnector;
    private final ClientEventHandler eventHandler;
    private final InstallationMonitor installationMonitor = new InstallationMonitor();

    private final FileHelper fileHelper = new FileHelper();

    class InstallationMonitor implements InstallationListener {

        private final AtomicBoolean active = new AtomicBoolean(true);

        @Override
        public void pause() {
            synchronized (active) {
                active.set(!active.get());
            }
        }

        public boolean isActive() {
            return active.get();
        }
    }

    public ClientInstaller(ConfigDb configDb, ApiConnector apiConnector) {
        this.configDb = configDb;
        this.apiConnector = apiConnector;
        this.eventHandler = ClientEventHandler.getInstance();
        eventHandler.register(installationMonitor);
    }

    public InstallationResult install(InstallerOptions options,
                                      Path workDir, String apiBaseUrl,
                                      Client.Status status,
                                      TokenSupplier tokenSupplier) {

        Client currentClient;
        Session session;
        Path installDir;

        if (!StringUtils.isEmpty(options.slug())) {
            final String slug = options.slug();
            currentClient = configDb.getClient(slug);
            if (currentClient == null) {
                panic("No client found: run 'install --username ...' first");
            }
            List<Token> tokens = configDb.getTokens(slug);
            if (tokens.isEmpty()) {
                panic("No client found: run 'install --username ...' first");
            }
            logger.debug("Found {} tokens for '{}'", tokens.size(), slug);
            validateTokens(tokens);
            final String token = tokens.get(0).token();
            session = Session.login(token, apiBaseUrl);
            installDir = currentClient.path();
        } else {
            session = Session.login(options.username(), options.password(), apiConnector, tokenSupplier);
            currentClient = configDb.getClient(session.getSlug());
            installDir = workDir;
        }
        logger.info("Running in path: {}", installDir);

        final String slug = session.getSlug();
        final Installation installation = new Installation(installDir, slug);

        new LoginService(configDb, apiConnector)
            .updateClientAndToken(session, currentClient, installDir, status);

        final List<Manifest.File> invalid = new ArrayList<>();
        final List<Manifest.File> missing = new ArrayList<>();
        // We list files, so empty directories will still remain
        final List<Path> currentFiles = getAllFiles(installation.getInstallPath());

        final List<Manifest.File> files = session.getManifestHandler(installation.getDownloadsPath()).getFiles();

        eventHandler.validationStart(files.size());

        for (int i = 0; i < files.size(); i++) {
            final Manifest.File file = files.get(i);
            logger.info("Processing file ({}/{}): {}", i + 1, files.size(), file.path());
            final Path location = installation.getInstallPath(file.path());
            if (currentFiles.contains(location)) {
                currentFiles.remove(location);
            }
            if (!location.toFile().exists()) {
                missing.add(file);
            } else {
                if (!hasValidCrc(location, file, options.fileCheck())) {
                    invalid.add(file);
                }
            }
            eventHandler.fileValidated();
        }

        // Summary
        if (!invalid.isEmpty()) {
            logger.info("Found {} invalid file(s) to patch", invalid.size());
            invalid.stream().forEach(s -> logger.info(Path.of(slug, s.path()).toString()));
        }
        if (!missing.isEmpty()) {
            logger.info("Found {} missing file(s) to install", missing.size());
            missing.stream().forEach(s -> logger.info(Path.of(slug, s.path()).toString()));
        }
        if (!currentFiles.isEmpty()) {
            logger.info("Found {} orphan file(s) to delete", currentFiles.size());
            currentFiles.stream().forEach(s -> logger.info(installDir.relativize(s).toString()));
        }

        // Actual installation
        eventHandler.filesToInstall(invalid.size() + missing.size());
        eventHandler.dataToDownload(getTotalDownloadSize(invalid, missing));
        eventHandler.dataToAssemble(getTotalPatchDataSize(invalid, missing));

        if (!invalid.isEmpty()) {
            installFiles(invalid, session, installation, options);
        }
        if (!missing.isEmpty()) {
            installFiles(missing, session, installation, options);
        }
        if (installationMonitor.isActive()) {
            if (!currentFiles.isEmpty()) {
                currentFiles.forEach(path -> path.toFile().delete());
            }

            configDb.updateClient(slug, session.getVersion(), UPDATED);
            logger.info("Installation completed");
        } else {
            configDb.updateClient(slug, session.getVersion(), status);
            logger.info("Installation stopped. Current status: {}", status);
        }
        // Force to clean memory
        System.gc();

        // TODO we don't use the result: remove or update with only rework when we separate downloading from extracting
        return new InstallationResult(invalid.size(), missing.size(), currentFiles.size());
    }

    private long getTotalDownloadSize(List<Manifest.File> invalid, List<Manifest.File> missing) {
        return downloadFileSize(invalid) + downloadFileSize(missing);
    }

    private static long downloadFileSize(List<Manifest.File> files) {
        return files.stream().mapToLong(Manifest.File::getBundlesSize).sum();
    }

    private long getTotalPatchDataSize(List<Manifest.File> invalid, List<Manifest.File> missing) {
        long invalidFilesSize = invalid.stream().mapToLong(Manifest.File::totalSize).sum();
        long missingFilesSize = missing.stream().mapToLong(Manifest.File::totalSize).sum();
        return invalidFilesSize + missingFilesSize;
    }

    private static boolean hasValidCrc(Path location, Manifest.File file, InstallerOptions.FileCheck fileCheck) {
        if (location.toFile().length() != file.totalSize()) {
            logger.debug("Invalid: expected size {}, found {}", file.totalSize(), location.toFile().length());
            return false;
        } else {
            final String calculatedCrc = switch (fileCheck) {
                case inmemory -> HashFunctions.InMemory.xxh3(location);
                default -> HashFunctions.OS.xxh3(location);
            };
            if (!calculatedCrc.equals(file.fileHash())) {
                logger.debug("Invalid: expected hash {}, found {}", file.fileHash(), calculatedCrc);
                return false;
            }
        }
        return true;
    }

    public void validateTokens(List<Token> tokens) {
        Optional<Token> activeToken = findToken(tokens, false);
        if (!activeToken.isPresent()) {
            panic("All token(s) expired: run 'install --username ...' to create a new one");
        }
    }

    private static Optional<Token> findToken(List<Token> tokens, boolean isExpired) {
        return tokens.stream()
            .filter(s -> JwtParser.parse(s.token()).isExpired() == isExpired)
            .findFirst();
    }

    record InstallationResult(int invalid, int missing, int orphan) {
    }

    // We could have async workers to download and extract in parallel
    private void installFiles(List<Manifest.File> files, Session session, Installation installation, InstallerOptions options) {
        for (Manifest.File file : files) {
            fileHelper.downloadChunks(file, session.getChunksUrl(), installation);
            if (!installationMonitor.isActive()) break;

            fileHelper.extract(file, installation, options.fileCheck());
            eventHandler.fileInstalled();
            eventHandler.dataAssembled(file.totalSize());
        }
    }

    private class FileHelper {

        private final ClientEventHandler eventHandler;

        private FileHelper() {
            this.eventHandler = ClientEventHandler.getInstance();
        }

        private void downloadChunks(Manifest.File file, String chunksUrl, Installation installation) {
            for (Manifest.Bundle bundle : file.getBundlesList()) {
                if (!installationMonitor.isActive()) {
                    logger.debug("Installation stopped");
                    break;
                }

                final String bundleName = bundle.bundleCrc() + ".bin";
                final Path downloadPath = installation.getBundlePath(bundleName);

                if (!fileExists(downloadPath)) {
                    logger.info("Downloading bundle: {}", downloadPath.toAbsolutePath());
                    Downloader.downloadFile(buildUrl(chunksUrl, bundleName).toString(), downloadPath);
                } else {
                    logger.info("Retrieved bundle from cache: {}", downloadPath.toAbsolutePath());
                }
                eventHandler.dataDownloaded(bundle.bundleLength());

                String crc = compact(HashFunctions.InMemory.crc64(downloadPath));
                if (!crc.equals(bundle.bundleCrc())) {
                    panic("CRC validation failed for: " + bundleName);
                }
            }
        }

        // manifest JSON returns crc's with missing 0's on the left side
        public String compact(String input) {
            int i = 0;
            while (i < input.length() - 1 && input.charAt(i) == '0') {
                i++;
            }
            return input.substring(i);
        }

        private void extract(Manifest.File file, Installation installation, InstallerOptions.FileCheck fileCheck) {
            final Path destination = installation.getInstallPath(file.path());
            FileUtils.createDirectories(destination);

            logger.debug("Assembling and extracting: {}", destination);
            Zstd.Section[] sections = file.getBundlesList()
                .stream()
                .map(bundle -> new Zstd.Section(installation.getBundlePath(bundle.resolveName()), bundle.fileSectionLength()))
                .toArray(Zstd.Section[]::new);
            Zstd.InMemory.decompress(destination, sections);

            if (!hasValidCrc(destination, file, fileCheck)) {
                panic("Could not validate file: " + destination);
            }
        }
    }

}
