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

    private static final Path GAME_SETTINGS = Path.of("drive_c")
        .resolve("users")
        .resolve("steamuser")
        .resolve("AppData")
        .resolve("LocalLow")
        .resolve("Niche Worlds Cult")
        .resolve("Monsters and Memories");

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void shouldUseSteamCompatibilityDataPathOnWindows() {
        ClientPaths paths = new ClientPaths(null, true);
        Path expected = Path.of("C:/steam-compat-data")
            .resolve(GAME_SETTINGS)
            .toAbsolutePath()
            .normalize();

        assertThat(paths.getGameSettingsPath()).isEqualTo(expected.toString());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldUseClientDirectoryAsWinePrefixOnLinux() {
        Client client = new Client("mnm", "1.2.3", UPDATED, Path.of("my-client"));
        ClientPaths paths = new ClientPaths(new ClientStatus(client, true, null), true);

        Path expected = client.path()
            .resolve(DEFAULT_MNM_PREFIX)
            .resolve(GAME_SETTINGS)
            .toAbsolutePath()
            .normalize();

        assertThat(paths.getGameSettingsPath()).isEqualTo(expected.toString());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldUseWorkingDirectoryAsWinePrefixOnLinux() {
        ClientPaths paths = new ClientPaths(null, false);

        Path expected = Environment.getWorkDir()
            .resolve(GAME_SETTINGS)
            .toAbsolutePath()
            .normalize();

        assertThat(paths.getGameSettingsPath()).isEqualTo(expected.toString());
    }

}
