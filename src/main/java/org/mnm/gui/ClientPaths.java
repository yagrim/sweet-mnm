package org.mnm.gui;

import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.config.Environment;
import org.mnm.config.OS;
import org.mnm.tools.StringUtils;

import static org.mnm.client.ClientRunner.winePrefixName;

class ClientPaths {

    private static final Logger logger = LoggerFactory.getLogger(ClientPaths.class);

    // Note: Windows 11 stores logs in "AppData/Local", in Proton all in the same path
    private static final String gameSettings = "pfx/drive_c/users/steamuser/AppData/LocalLow/Niche Worlds Cult/Monsters and Memories";

    private static final String STEAM_COMPAT_DATA_PATH_ERROR = "Error: STEAM_COMPAT_DATA_PATH empty or not set";
    private static final String INSTALLATION_NOT_FOUND_ERROR = "Error: installation not found, run Install or Repair";

    private final Optional<String> winePrefix;
    // Prefix depends on the client slug, so it cannot be resolved without client
    private final boolean missingClient;

    ClientPaths(ClientStatus clientStatus, boolean clientAsWinePrefix) {
        missingClient = !OS.isWindows() && clientAsWinePrefix && (clientStatus == null || clientStatus.client() == null);
        winePrefix = missingClient ? Optional.empty() : getWinePrefixLocation(clientStatus, clientAsWinePrefix);
    }

    String getGameSettingsPath() {
        if (missingClient) {
            return INSTALLATION_NOT_FOUND_ERROR;
        }
        return winePrefix
            .map(prefix -> {
                String path = prefix + "/" + gameSettings;
                return exists(path) ? path : INSTALLATION_NOT_FOUND_ERROR;
            })
            .orElse(STEAM_COMPAT_DATA_PATH_ERROR);
    }

    private boolean exists(String path) {
        Path absolutePath = Path.of(path).normalize().toAbsolutePath();
        logger.debug("Exists? {}", absolutePath);
        if (absolutePath.toFile().exists()) {
            return true;
        }
        if (OS.isWindows()) {
            // Proton hotfix
            Path zDrivePath = protonPath(path);
            logger.debug("Exists? {}", zDrivePath);
            return zDrivePath.toFile().exists();
        }
        return false;
    }

    /**
     * Returns sanitized Proton Path.
     */
    static Path protonPath(String path) {
        Path absolutePath = Path.of(path).normalize().toAbsolutePath();
        if (absolutePath.toFile().exists()) {
            return absolutePath;
        }
        return PathUtils.replaceDrive(absolutePath, 'z');
    }

    private Optional<String> getWinePrefixLocation(ClientStatus client, boolean clientAsWinePrefix) {
        // TODO Support real windows and not only Proton?
        if (OS.isWindows()) {
            String compatDataPath = System.getenv("STEAM_COMPAT_DATA_PATH");
            return StringUtils.isEmpty(compatDataPath) ? Optional.empty() : Optional.of(compatDataPath);
        }
        if (clientAsWinePrefix) {
            return Optional.of(client.client().path().toAbsolutePath() + "/" + winePrefixName(client.client().slug()));
        } else {
            return Optional.of(Environment.getWorkDir().toString());
        }
    }

}
