package org.mnm.gui;

import java.nio.file.Path;
import java.util.Optional;

import org.mnm.config.Environment;
import org.mnm.config.OS;

import static org.mnm.client.ClientRunner.DEFAULT_MNM_PREFIX;

class ClientPaths {

    // Note: Windows 11 stores logs in "AppData/Local", in Proton all in the same path
    private static final String gameSettings = "./drive_c/users/steamuser/AppData/LocalLow/Niche Worlds Cult/Monsters and Memories";

    private final Optional<Path> winePrefix;

    ClientPaths(ClientStatus clientStatus, boolean clientAsWinePrefix) {
        winePrefix = getWinePrefixLocation(clientStatus, clientAsWinePrefix);
    }

    private Optional<Path> getWinePrefixLocation(ClientStatus client, boolean clientAsWinePrefix) {
        if (OS.isWindows()) {
            return Optional.ofNullable(System.getenv("STEAM_COMPAT_DATA_PATH"))
                .filter(path -> !path.isEmpty())
                .map(Path::of);
        }
        if (clientAsWinePrefix) {
            return Optional.of(client.client().path().resolve(DEFAULT_MNM_PREFIX).toAbsolutePath());
        } else {
            return Optional.of(Environment.getWorkDir().toAbsolutePath());
        }
    }

    String getGameSettingsPath() {
        return winePrefix
            .map(prefix -> prefix.resolve(gameSettings).toAbsolutePath().normalize().toString())
            .orElse("Error: STEAM_COMPAT_DATA_PATH empty or not set");
    }

}
