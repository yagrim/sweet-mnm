package org.mnm.client;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.api.Session;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.Token;
import org.mnm.manifest.Manifest;
import org.mnm.tools.Downloader;
import org.mnm.tools.FileUtils;
import org.mnm.tools.HashFunctions;
import org.mnm.tools.JwtParser;
import org.mnm.tools.StringUtils;
import org.mnm.tools.Zstd;

import static org.mnm.config.Client.Status.COMPLETED;
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
        // Instead, maybe have an OPS table to audit what was the last operation
        void install(InstallerOptions options, ConfigDb configDb, Client.Status status);
    }

    private final ConfigDb configDb;

    public ClientInstaller(ConfigDb configDb) {
        this.configDb = configDb;
    }

    public InstallationResult install(InstallerOptions options,
                                      Path workDir, String apiBaseUrl,
                                      Client.Status status) {

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
            session = Session.login(options.username(), options.password(), apiBaseUrl);
            currentClient = configDb.getClient(session.getSlug());
            installDir = workDir;
        }
        logger.info("Running in path: {}", installDir);

        final String slug = session.getSlug();
        final Installation installation = new Installation(installDir, slug);

        new LoginService(configDb)
            .storeToken(session, currentClient, installDir, status);

        final List<Manifest.File> invalid = new ArrayList<>();
        final List<Manifest.File> missing = new ArrayList<>();
        // We list files, so empty directories will still remain
        final List<Path> currentFiles = getAllFiles(installation.getInstallPath());

        for (Manifest.File file : session.getManifestHandler(installation.getDownloadsPath()).getFiles()) {
            final Path location = installation.getInstallPath(file.path());
            if (currentFiles.contains(location)) {
                currentFiles.remove(location);
            }
            if (!location.toFile().exists()) {
                missing.add(file);
            } else {
                if (location.toFile().length() != file.totalSize()) {
                    logger.debug("Invalid: expected size {}, found {}", file.totalSize(), location.toFile().length());
                    invalid.add(file);
                } else {
                    final String calculatedCrc = switch (options.fileCheck()) {
                        case inmemory -> HashFunctions.InMemory.xxh3(location);
                        default -> HashFunctions.OS.xxh3(location);
                    };
                    if (!calculatedCrc.equals(file.fileHash())) {
                        logger.debug("Invalid: expected hash {}, found {}", file.fileHash(), calculatedCrc);
                        invalid.add(file);
                    }
                }
            }
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
        if (!invalid.isEmpty()) {
            installFiles(invalid, session, installation);
        }
        if (!missing.isEmpty()) {
            installFiles(missing, session, installation);
        }
        if (!currentFiles.isEmpty()) {
            currentFiles.forEach(path -> path.toFile().delete());
        }

        configDb.updateClientStatus(slug, COMPLETED);

        // Force to clean memory
        System.gc();

        return new InstallationResult(invalid.size(), missing.size(), currentFiles.size());
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
                logger.info("Downloading bundle: {}", downloadPath.toAbsolutePath());

                if (!fileExists(downloadPath)) {
                    Downloader.downloadFile(buildUrl(chunksUrl, bundleName).toString(), downloadPath);
                }

                String crc = compact(HashFunctions.InMemory.crc64(downloadPath));
                if (!crc.equals(bundle.bundleCrc())) {
                    panic("CRC validation failed for: " + bundleName);
                }
            }
        }

        // manifest JSON returns crc's with missing 0's on the left side
        public static String compact(String input) {
            int i = 0;
            while (i < input.length() - 1 && input.charAt(i) == '0') {
                i++;
            }
            return input.substring(i);
        }

        private static void extract(Manifest.File file, Installation installation) {
            final Path destination = installation.getInstallPath(file.path());
            FileUtils.createDirectories(destination);

            logger.debug("Assembling: {}", destination);
            Zstd.Section[] sections = file.getBundlesList()
                .stream()
                .map(bundle -> new Zstd.Section(installation.getBundlePath(bundle.resolveName()), bundle.fileSectionLength()))
                .toArray(Zstd.Section[]::new);
            Zstd.InMemory.decompress(destination, sections);
        }
    }

}
