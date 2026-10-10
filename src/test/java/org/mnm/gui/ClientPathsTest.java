package org.mnm.gui;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import org.mnm.config.Client;
import org.mnm.config.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.client.ClientRunner.DEFAULT_MNM_PREFIX;
import static org.mnm.config.Client.Status.UPDATED;

class ClientPathsTest {

    private static final String GAME_SETTINGS = "drive_c/users/steamuser/AppData/LocalLow/Niche Worlds Cult/Monsters and Memories";

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void shouldUseSteamCompatibilityDataPathOnWindows() {
        ClientPaths paths = new ClientPaths(null, true);
        String expected = "C:/steam-compat-data/" + GAME_SETTINGS;

        assertThat(paths.getGameSettingsPath()).isEqualTo(expected);
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldUseClientDirectoryAsWinePrefixOnLinux() {
        Client client = new Client("mnm", "1.2.3", UPDATED, Path.of("my-client"));
        ClientPaths paths = new ClientPaths(new ClientStatus(client, true, null), true);

        String expected = client.path().toAbsolutePath() + "/" + DEFAULT_MNM_PREFIX + "/" + GAME_SETTINGS;

        assertThat(paths.getGameSettingsPath()).isEqualTo(expected);
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldUseWorkingDirectoryAsWinePrefixOnLinux() {
        ClientPaths paths = new ClientPaths(null, false);

        String expected = Environment.getWorkDir() + "/" + GAME_SETTINGS;

        assertThat(paths.getGameSettingsPath()).isEqualTo(expected);
    }

}
