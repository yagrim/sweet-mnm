package org.mnm;

import org.mnm.config.Environment;
import org.mnm.manifest.Manifest;
import org.mnm.manifest.ManifestHandler;
import org.mnm.tools.HashFunctions;

import java.nio.file.Path;
import java.util.List;

public class InstallationValidation {

    public static final Path INSTALLATION = Environment.downloads.resolve("mnm");

    public static void main(String[] args) {

        ManifestService manifestService = new ManifestService();
        ManifestHandler manifestHandler = manifestService.getManifestHandler(args[0], args[1]);

//        ApiConnection.GameVersion gameVersion = gamesVersions.get(0);
//        System.out.println("Processing Game Version: " + gameVersion.slug() + "@" + gameVersion.version());

        new InstallationValidator()
                .validate(manifestHandler);
    }

    static class InstallationValidator {

        void validate(ManifestHandler manifestHandler) {
            validateInstallation(manifestHandler);
        }

        /**
         * Traverses manifes to validate installed files validating the expected size and hash.
         */
        private void validateInstallation(ManifestHandler manifestHandler) {
            final List<Manifest.File> files = manifestHandler.getFiles();
            for (Manifest.File file : files) {

                final Path asset = INSTALLATION.resolve(file.path().substring(1));

                int totalFileSize = file.totalSize();
                if (asset.toFile().length() != totalFileSize) {
                    System.out.println("Found inconsistent file. Incorrect size, expected " + totalFileSize + ", got " + asset.toFile().length());
                }

                final String hash = HashFunctions.xxh3(asset);
                if (!hash.equals(file.fileHash())) {
                    System.out.println("Found inconsistent file. Incorrect hash, expected " + file.fileHash() + ", got " + hash);
                }
            }

            System.out.printf("Validated %d files\n", files.size());
        }
    }

}
