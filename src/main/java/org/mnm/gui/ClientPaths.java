package org.mnm.gui;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import org.mnm.config.Environment;
import org.mnm.config.OS;
import org.mnm.tools.StringUtils;

import static org.mnm.client.ClientRunner.DEFAULT_MNM_PREFIX;

class ClientPaths {

    // Note: Windows 11 stores logs in "AppData/Local", in Proton all in the same path
    private static final String gameSettings = "pfx/drive_c/users/steamuser/AppData/LocalLow/Niche Worlds Cult/Monsters and Memories";

    private static final String STEAM_COMPAT_DATA_PATH_ERROR = "Error: STEAM_COMPAT_DATA_PATH empty or not set";
    private static final String INSTALLATION_NOT_FOUND_ERROR = "Error: installation not found, run Install or Repair";

    private final Optional<String> winePrefix;

    ClientPaths(ClientStatus clientStatus, boolean clientAsWinePrefix) {
        winePrefix = getWinePrefixLocation(clientStatus, clientAsWinePrefix);
    }

    String getGameSettingsPath() {
        return winePrefix
            .map(prefix -> {
                String path = prefix + "/" + gameSettings;
                return isExists(path) ? path : INSTALLATION_NOT_FOUND_ERROR;
            })
            .orElse(STEAM_COMPAT_DATA_PATH_ERROR);
    }

    private static boolean isExists(String path) {
        return Path.of(path).normalize().toAbsolutePath().toFile().exists();
    }

    private Optional<String> getWinePrefixLocation(ClientStatus client, boolean clientAsWinePrefix) {
        // TODO Support real windows and not only Proton?
        if (OS.isWindows()) {
            String compatDataPath = System.getenv("STEAM_COMPAT_DATA_PATH");
            return StringUtils.isEmpty(compatDataPath) ? Optional.empty() : Optional.of(compatDataPath);
        }
        if (clientAsWinePrefix) {
            return Optional.of(client.client().path().toAbsolutePath() + "/" + DEFAULT_MNM_PREFIX);
        } else {
            return Optional.of(Environment.getWorkDir().toString());
        }
    }
}
