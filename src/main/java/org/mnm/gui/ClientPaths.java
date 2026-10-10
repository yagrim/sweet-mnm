package org.mnm.gui;

import java.util.Optional;

import org.mnm.config.Environment;
import org.mnm.config.OS;
import org.mnm.tools.StringUtils;

import static org.mnm.client.ClientRunner.DEFAULT_MNM_PREFIX;

class ClientPaths {

    // TODO before installing the files does not exists
    // We should check if the file exists and if doesn't: show special message and disable "copy" button
    // Note: Windows 11 stores logs in "AppData/Local", in Proton all in the same path
    private static final String gameSettings = "pfx/drive_c/users/steamuser/AppData/LocalLow/Niche Worlds Cult/Monsters and Memories";
    private static final String STEAM_COMPAT_DATA_PATH_ERROR = "Error: STEAM_COMPAT_DATA_PATH empty or not set";

    private final Optional<String> winePrefix;

    ClientPaths(ClientStatus clientStatus, boolean clientAsWinePrefix) {
        winePrefix = getWinePrefixLocation(clientStatus, clientAsWinePrefix);
    }

    private Optional<String> getWinePrefixLocation(ClientStatus client, boolean clientAsWinePrefix) {
        // TODO Support real windows?
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

    String getGameSettingsPath() {
        return winePrefix
            .map(prefix -> prefix + "/" + gameSettings)
            .orElse(STEAM_COMPAT_DATA_PATH_ERROR);
    }

}
