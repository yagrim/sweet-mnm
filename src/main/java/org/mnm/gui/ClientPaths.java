package org.mnm.gui;

import java.nio.file.Path;

import org.mnm.config.Environment;
import org.mnm.config.OS;

import static org.mnm.client.ClientRunner.DEFAULT_MNM_PREFIX;

class ClientPaths {

    // Note: Windows 11 stores logs in "AppData/Local", in Proton all in the same path
    private static final String gameSettings = "./drive_c/users/steamuser/AppData/LocalLow/Niche Worlds Cult/Monsters and Memories";

    private final Path winePrefix;

    ClientPaths(ClientStatus clientStatus, boolean clientAsWinePrefix) {
        winePrefix = getWinePrefixLocation(clientStatus, clientAsWinePrefix);
    }

    private Path getWinePrefixLocation(ClientStatus client, boolean clientAsWinePrefix) {
        if (OS.isWindows()) {
            return Path.of(System.getenv("STEAM_COMPAT_DATA_PATH"));
        }
        if (clientAsWinePrefix) {
            return client.client().path().resolve(DEFAULT_MNM_PREFIX).toAbsolutePath();
        } else {
            return Environment.getWorkDir().toAbsolutePath();
        }
    }

    String getGameSettingsPath() {
        return winePrefix.resolve(gameSettings).toAbsolutePath().normalize().toString();
    }

}
