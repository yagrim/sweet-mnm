package org.mnm.gui;

import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.config.Environment;
import org.mnm.config.OS;
import org.mnm.config.WinePrefix;
import org.mnm.tools.StringUtils;

class ClientPaths {

    private static final Logger logger = LoggerFactory.getLogger(ClientPaths.class);

    // Path relative to the user home directory, on Windows and in a Wine prefix
    private static final String USER_GAME_SETTINGS = "AppData/LocalLow/Niche Worlds Cult/Monsters and Memories";
    private static final String gameSettings = "pfx/drive_c/users/steamuser/" + USER_GAME_SETTINGS;

    private static final String INSTALLATION_NOT_FOUND_ERROR = "Error: installation not found, run Install or Repair";

    private final Optional<String> winePrefix;

    private final boolean missingClient;

    private final String userHome;

    ClientPaths(ClientStatus clientStatus, boolean clientAsWinePrefix) {
        this(clientStatus, clientAsWinePrefix, System.getenv("STEAM_COMPAT_DATA_PATH"), System.getProperty("user.home"));
    }

    ClientPaths(ClientStatus clientStatus, boolean clientAsWinePrefix, String compatDataPath, String userHome) {
        this.userHome = userHome;
        missingClient = !OS.isWindows() && clientAsWinePrefix && (clientStatus == null || clientStatus.client() == null);
        winePrefix = missingClient ? Optional.empty() : getWinePrefixLocation(clientStatus, clientAsWinePrefix, compatDataPath);
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
            .orElseGet(this::getUserGameSettingsPath);
    }

    // Without STEAM_COMPAT_DATA_PATH, Sweet runs on Windows, or in Proton outside of Steam.
    // In both cases, the game settings are in the home directory of the user.
    private String getUserGameSettingsPath() {
        String path = userHome + "/" + USER_GAME_SETTINGS;
        return exists(path) ? path : INSTALLATION_NOT_FOUND_ERROR;
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

    private Optional<String> getWinePrefixLocation(ClientStatus client, boolean clientAsWinePrefix, String compatDataPath) {
        if (OS.isWindows()) {
            return StringUtils.isEmpty(compatDataPath) ? Optional.empty() : Optional.of(compatDataPath);
        }
        if (clientAsWinePrefix) {
            return Optional.of(client.client().path().toAbsolutePath() + "/" + WinePrefix.name(client.client().slug()));
        } else {
            return Optional.of(Environment.getWorkDir().toString());
        }
    }

}
